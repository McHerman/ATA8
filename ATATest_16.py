#!/usr/bin/env python3
import os
import struct
import json

def write_to_device(fd, byte_data):
    os.write(fd, byte_data)

def read_from_device(fd):
    raw_data = os.read(fd, 256)
    data = [struct.unpack('<Q', raw_data[i:i+8])[0] for i in range(0, len(raw_data), 8)]
    return data

def read_debug_state(fd, state_name, state_address):
    # The state_address needs to be the byte address, so if your state_address
    # is in 32-bit words you need to multiply by 4 to get the byte address.
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
    # Open files
    DATA_WRITE = os.open("/dev/xdma0_h2c_0", os.O_WRONLY)
    INST_WRITE = os.open("/dev/xdma0_h2c_1", os.O_WRONLY)
    DEBUG_READ = os.open("/dev/xdma0_user", os.O_RDONLY)    
    fd_c2h = os.open("/dev/xdma0_c2h_0", os.O_RDONLY)

    # Read all states from the debug interface
    #read_all_states(DEBUG_READ, 'config.json')

    # Generate instructions as bytes
    #instructions = struct.pack('<Q', 134217730) + struct.pack('<Q', 34493956098) + struct.pack('<Q', 4503633987110913) + struct.pack('<Q',68853694467)
    instructions1 = struct.pack('<Q', 268435458) + struct.pack('<Q', 17592454479874) + struct.pack('<Q', 2305860601399742465) + struct.pack('<Q', 35184640524291) 

    write_to_device(INST_WRITE, instructions1)
    print("Instruction1 written")

    instructions2 = struct.pack('<Q', 1099780063234) + struct.pack('<Q', 18691966107650) + struct.pack('<Q', 2377919294966075393) + struct.pack('<Q',36284152152067)

    write_to_device(INST_WRITE, instructions2)
    print("Instruction2 written")

    #read_all_states(DEBUG_READ, 'config.json')

    # Generate 8x8 matrices as bytes and send them
    matrix1 = struct.pack('<Q', 0x0102030401020304) * 32
    write_to_device(DATA_WRITE, matrix1)
    print("Data1 written")

    matrix2 = struct.pack('<Q', 0x0102030401020304) * 32
    write_to_device(DATA_WRITE, matrix2)
    print("Data2 written")

    matrix3 = struct.pack('<Q', 0x0102030401020304) * 32
    write_to_device(DATA_WRITE, matrix3)
    print("Data3 written")

    matrix4 = struct.pack('<Q', 0x0102030401020304) * 32
    write_to_device(DATA_WRITE, matrix4)
    print("Data4 written")


    #read_all_states(DEBUG_READ, 'config.json')

    # Read 8 separate 64-bit integers from FPGA
    read_data = read_from_device(fd_c2h)
    print("Read Data: ", read_data)

    read_data2 = read_from_device(fd_c2h)
    print("Read Data: ", read_data2)

    # Close device files
    os.close(DATA_WRITE)
    os.close(INST_WRITE)
    os.close(fd_c2h)
    os.close(DEBUG_READ)

if __name__ == '__main__':
    main()
