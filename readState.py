#!/usr/bin/env python3
import os
import struct
import json

def read_debug_state(fd, state_name, state_address):
    byte_address = state_address * 4
    raw_data = os.pread(fd, 4, byte_address)
    state_value = struct.unpack('<I', raw_data)[0]
    print(f"{state_name} state: {state_value}")

def read_all_states(fd, config_path):
    with open(config_path, 'r') as f:
        states = json.load(f)
    for state_name, state_address in states.items():
        read_debug_state(fd, state_name, state_address)

def main():
    # Open the device file
    fd = os.open("/dev/xdma0_user", os.O_RDONLY)
    try:
        # Read and print all states from the debug interface
        read_all_states(fd, 'config.json')
    finally:
        # Close the device file
        os.close(fd)

if __name__ == '__main__':
    main()
