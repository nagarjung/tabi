# python notifybgp.py --data-dir /home/bsd/working --bgp-rib bgp_rib --bgp-updates bgp_updates &

from __future__ import print_function

import argparse
import logging
import os
from subprocess import check_call
import time

from watchdog.events import FileSystemEventHandler
from watchdog.observers import Observer

script_dir = os.path.dirname(os.path.abspath(__file__))


def make_dir(dir_name, new_dir):
    destination_dir = os.path.join(dir_name, new_dir)

    try:
        os.makedirs(destination_dir)
    except OSError:
        pass  # already exists

    return destination_dir


logger = logging.getLogger(__name__)

log_dir = make_dir(script_dir, "logs")
file_name = "router.log"
log_file = log_dir+"/"+file_name

logger.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s:%(name)s:%(message)s')
file_handler = logging.FileHandler(log_file)
file_handler.setLevel(logging.DEBUG)
file_handler.setFormatter(formatter)
logger.addHandler(file_handler)


def return_command(src_path):

    gzip_cmd = "gzip " + src_path
    dst_path = "pramati@103.54.143.6:/home/pramati/"
    rsync_cmd = 'rsync -avz -e "ssh -i /auth/tabi.pem -p 1128" ' + src_path+".gz"+" " + dst_path
    command = gzip_cmd + " ; " + rsync_cmd

    return command


class PollingHandler(FileSystemEventHandler):

    def on_created(self, event):

        super(PollingHandler, self).on_created(event)
        what = 'directory' if event.is_directory else 'file'

        if not event.src_path.endswith(".gz"):

            if rib_path == os.path.dirname(event.src_path):
                """ If an event is triggered in RIB path
                    gzip the file and rync it to Tabi machine"""

                temp_size = -1
                while temp_size != os.path.getsize(event.src_path):
                    temp_size = os.path.getsize(event.src_path)
                    time.sleep(2)

                logger.info(" %s file generated" % event.src_path)

                command = return_command(event.src_path)
                check_call(command, shell=True)

                logger.info(" %s file is gziped and rsynced to tabi machine" % event.src_path)

            if updates_path == os.path.dirname(event.src_path):
                """If an event is triggering bgp path, read files
                 and load them into radix trees """

                temp_size = -1
                while temp_size != os.path.getsize(event.src_path):
                    temp_size = os.path.getsize(event.src_path)
                    time.sleep(2)

                logger.info(" %s file generated" % event.src_path)

                command = return_command(event.src_path)
                check_call(command, shell=True)

                logger.info(" %s file is gziped and rsynced to tabi machine" % event.src_path)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument("--data-dir",
                        help="enter the path for RIB and Updates folder")

    parser.add_argument("--bgp-rib",
                        help="enter the name for BGP RIB data folder")

    parser.add_argument("--bgp-updates",
                        help="enter the name for BGP update data folder")

    args = parser.parse_args()

    rib_path = make_dir(args.data_dir, args.bgp_rib)
    updates_path = make_dir(args.data_dir, args.bgp_updates)

    targets = [rib_path, updates_path]

    event_handler = PollingHandler()
    observer = Observer()
    observers = []

    for path in targets:
        targetPath = str(path)
        observer.schedule(event_handler, targetPath, recursive=False)
        observers.append(observer)

    logger.info(" BGP router notify Process started")
    observer.start()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()

    observer.join()

