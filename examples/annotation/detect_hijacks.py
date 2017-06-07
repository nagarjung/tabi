# -*- coding: utf-8 -*-
# Copyright (C) 2016 ANSSI
# This file is part of the tabi project licensed under the MIT license.

from __future__ import print_function

import argparse
import json
import logging
import os
import time

from time import localtime, strftime
from tabi.rib import EmulatedRIB
from tabi.emulator import parse_registry_data, detect_hijacks, make_dir, \
    generate_registry_events, generate_rib_event
from watchdog.events import FileSystemEventHandler
from watchdog.observers import Observer

script_dir = os.path.dirname(os.path.abspath(__file__))

logger = logging.getLogger(__name__)

log_dir = make_dir(script_dir, "bgp_logs")
file_name = "bgp.log"
log_file = log_dir+"/"+file_name

logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s:%(name)s:%(message)s')
file_handler = logging.FileHandler(log_file)
file_handler.setLevel(logging.DEBUG)
file_handler.setFormatter(formatter)
logger.addHandler(file_handler)


def choose_input(input):
    if input == "mabo":
        from tabi.input.mabo import mabo_input
        return mabo_input

    elif input == "bgpreader":
        from tabi.input.bgpreader import bgpreader_input
        return bgpreader_input

    else:
        raise ValueError("unknown input type {}".format(input))


def registry_kwargs(kwargs, file_path):
    if os.path.basename(file_path) == "routes.csv":
        kwargs["irr_ro_file"] = file_path

    if os.path.basename(file_path) == "roa.csv":
        kwargs["rpki_roa_file"] = file_path

    if os.path.basename(file_path) == "maintainers.csv":
        kwargs["irr_mnt_file"] = file_path

    if os.path.basename(file_path) == "organisations.csv":
        kwargs["irr_org_file"] = file_path

    return kwargs


class PollingHandler(FileSystemEventHandler):

    def __init__(self):
        self.kwargs = {}
        self.mrt_files = []
        self.list_funcs = []
        self.rib = EmulatedRIB()
        self.rec = {}

    def on_created(self, event):
        super(PollingHandler, self).on_created(event)
        what = 'directory' if event.is_directory else 'file'
        logger.info(" Created %s: %s", what, event.src_path)

        if registry_dir == os.path.dirname(event.src_path):

            """If an event is triggering the registry path
            then load the registry data in structures and radix trees"""

            reg_kwargs = registry_kwargs(self.kwargs, event.src_path)

            if len(reg_kwargs) == 4:
                self.list_funcs = parse_registry_data(**reg_kwargs)
                logger.info(" Completed parsing registry data")

        if bgp_dir == os.path.dirname(event.src_path):

            """If an event is triggering bgp path, read files
             and load them into radix trees """

            ''' 
            historicalsize = -1
            while historicalsize != os.path.getsize(event.src_path):
                historicalsize = os.path.getsize(event.src_path)
                time.sleep(2)
            '''

            time.sleep(3)
            filepath = os.path.splitext(event.src_path)[0]
            file_dir = os.path.dirname(filepath)
            filename = os.path.basename(filepath)
            filename = file_dir+"/"+filename[1:]

            logger.info(" BGP file %s for processing" % filename)

            self.mrt_files = []
            self.mrt_files.append(filename)
            input_kwargs = {"files": self.mrt_files}
            input = choose_input(args.input)
            bgp_kwargs = input(args.collector, **input_kwargs)
            bgp_kwargs["rib"] = self.rib

            actual_time = strftime("%Y-%m-%d-%H-%M-%S", localtime())
            hijack_file = "hijacks-" + actual_time + ".log"
            result_dir = make_dir(script_dir, "results")
            hijacks_path = result_dir+"/"+hijack_file

            logger.info(" BGP processing started on file %s : " % filename)
            execution_time = time.time()
            with open(hijacks_path, "w") as outfile:
                for conflict in detect_hijacks(self.list_funcs, **bgp_kwargs):
                    if conflict["type"] == "ABNORMAL":
                        json.dump(conflict, outfile)
                        outfile.write('\n')

            logger.info(" Hijacks completed")
            logger.info(" Total execution time in seconds : %s" % (time.time() - execution_time))
            logger.info("-----------------------------------------------------------------------")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument("-c", "--collector",
                        help="collector name from where the log files are",
                        default="none")
    parser.add_argument("-i", "--input",
                        help="MRT parser, e.g. 'mabo'",
                        default="mabo")

    parser.add_argument("--registry-path",
                        help="enter a directory name for registry")

    parser.add_argument("--bgp-path",
                        help="enter a directory name for bgp")

    parser.add_argument("-v", "--verbose", action="store_true",
                        help="more logging")

    args = parser.parse_args()

    logger.info(' BGP Process started')

    registry_dir = make_dir(script_dir, args.registry_path)
    bgp_dir = make_dir(script_dir, args.bgp_path)

    targets = [registry_dir, bgp_dir]
    logger.info("watch directory for BGP data: %s" % bgp_dir)
    print(("watch directory for BGP data: %s" % bgp_dir))
    print("Please mention the above path in BGP router for rsync")

    event_handler = PollingHandler()
    observer = Observer()
    observers = []

    for path in targets:
        targetPath = str(path)
        observer.schedule(event_handler, targetPath, recursive=False)
        observers.append(observer)

    observer.start()
    dst_dir = script_dir+"/registry"
    generate_registry_events(script_dir, dst_dir)
    generate_rib_event(bgp_dir)

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()

    observer.join()
    logger.info("BGP Process Killed")
