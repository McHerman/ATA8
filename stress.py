#!/usr/bin/env python3
import os
import struct
import time
import datetime

def write_to_device(fd, byte_data):
    os.write(fd, byte_data)

def read_from_device(fd):
    raw_data = os.read(fd, 256)
    data = [struct.unpack('<Q', raw_data[i:i+8])[0] for i in range(0, len(raw_data), 8)]
    return data

def main():
    NUM_OUTER_ITERATIONS = 4
    NUM_INNER_ITERATIONS = 16
    INSTRUCTION_FILE = "rustTestProgram_Out_int.txt"
    #OUTPUT_FILE = "stressOutput_4.txt"
    current_time = datetime.datetime.now().strftime("%H_%M_%d_%m_%Y")
    OUTPUT_FILE = f"stresstest_{NUM_OUTER_ITERATIONS}_{current_time}.txt"

    DATA_WRITE = os.open("/dev/xdma0_h2c_0", os.O_WRONLY)
    INST_WRITE = os.open("/dev/xdma0_h2c_1", os.O_WRONLY)
    fd_c2h = os.open("/dev/xdma0_c2h_0", os.O_RDONLY)

    with open(OUTPUT_FILE, "w") as output_file, open(INSTRUCTION_FILE, "r") as file:
        instructions = file.readlines()
        total_start_time = time.time()

        for _ in range(NUM_OUTER_ITERATIONS):
            for iteration in range(NUM_INNER_ITERATIONS):
                start_time = time.time()

                # Write instructions
                instruction_block = instructions[iteration*4:(iteration+1)*4]
                packed_instructions = b''.join(struct.pack('<Q', int(instr.strip())) for instr in instruction_block)

                write_to_device(INST_WRITE, packed_instructions)

                # Pack and write matrix data
                matrix_data = struct.pack('<Q', 0x0102030401020304) * 32
                write_to_device(DATA_WRITE, matrix_data)
                write_to_device(DATA_WRITE, matrix_data)

                # Read and log data
                read_data = read_from_device(fd_c2h)
                end_time = time.time()

                output_file.write(f"Iteration {iteration + 1}:\n")
                output_file.write(f"Read Data: {read_data}\n")
                output_file.write(f"Time for iteration: {end_time - start_time} seconds\n\n")

        total_end_time = time.time()
        output_file.write(f"Total execution time: {total_end_time - total_start_time} seconds\n")
        output_file.write(f"Average time per iteration: {(total_end_time - total_start_time) / (NUM_OUTER_ITERATIONS * NUM_INNER_ITERATIONS)} seconds\n")

    os.close(DATA_WRITE)
    os.close(INST_WRITE)
    os.close(fd_c2h)

if __name__ == '__main__':
    main()
