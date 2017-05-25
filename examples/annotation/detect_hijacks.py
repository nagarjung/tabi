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
from tabi.emulator import parse_registry_data, detect_hijacks, make_dir
from watchdog.events import FileSystemEventHandler
from watchdog.observers import Observer

logger = logging.getLogger(__name__)

log_path = make_dir("bgp_logs", "bgp.log")
logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s:%(name)s:%(message)s')
file_handler = logging.FileHandler(log_path)
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

        if args.registry_path == os.path.dirname(event.src_path):
            """If an event is triggering the registry path
            then load the registry data in structures and radix trees"""

            reg_kwargs = registry_kwargs(self.kwargs, event.src_path)

            if len(reg_kwargs) == 4:
                self.list_funcs = parse_registry_data(**reg_kwargs)
                logger.info(" Completed parsing registry data")

        if args.bgp_path == os.path.dirname(event.src_path):
            """If an event is triggering bgp path, read files
             and load them into radix trees """

            self.mrt_files.append(event.src_path)
            input_kwargs = {"files": self.mrt_files}
            input = choose_input(args.input)
            bgp_kwargs = input(args.collector, **input_kwargs)
            bgp_kwargs["rib"] = self.rib

            actual_time = strftime("%Y-%m-%d-%H-%M-%S", localtime())
            file_name = "hijacks-" + actual_time + ".log"

            hijacks_path = make_dir("results", file_name)
            execution_time = time.time()

            logger.info(" BGP processing started")
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
                        help="enter the path for registry data")

    parser.add_argument("--bgp-path",
                        help="enter the path for bgp rib and update files")

    parser.add_argument("-v", "--verbose", action="store_true",
                        help="more logging")

    args = parser.parse_args()

    logger.info(' start logging')

    targets = [args.registry_path, args.bgp_path]
    event_handler = PollingHandler()
    observer = Observer()
    observers = []

    for path in targets:
        targetPath = str(path)
        observer.schedule(event_handler, targetPath, recursive=False)
        observers.append(observer)

    observer.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()

    observer.join()
