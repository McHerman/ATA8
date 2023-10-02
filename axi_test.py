import mmap
import os
import struct

def send_and_receive(n):
    DEVICE_PATH = "/dev/xdma0_h2c_0"
    C2H_PATH = "/dev/xdma0_c2h_0"
    INT_SIZE = 8  # 64-bits = 8 bytes
    BUFFER_SIZE = n * INT_SIZE

    # Generate n random 64-bit integers
    integers_to_send = [i for i in range(n)]
    
    # Convert the integers to byte data using struct
    byte_data = b"".join([struct.pack('Q', i) for i in integers_to_send])
    
    # Send data to FPGA
    with open(DEVICE_PATH, "r+b") as f:
        mmapped_data = mmap.mmap(f.fileno(), BUFFER_SIZE, access=mmap.ACCESS_WRITE)
        mmapped_data.write(byte_data)
        mmapped_data.flush()  # Ensure data is written
        mmapped_data.close()  # Unmap the memory

    # Read back data from FPGA
    received_integers = []
    with open(C2H_PATH, "rb") as f:
        mmapped_data_r = mmap.mmap(f.fileno(), BUFFER_SIZE, access=mmap.ACCESS_READ)
        received_byte_data = mmapped_data_r.read(BUFFER_SIZE)
        mmapped_data_r.close()
        
        # Convert byte data back to integers
        for i in range(n):
            start_idx = i * INT_SIZE
            end_idx = (i+1) * INT_SIZE
            integer, = struct.unpack('Q', received_byte_data[start_idx:end_idx])
            received_integers.append(integer)
    
    return received_integers

# Example usage:
n = 512
received_data = send_and_receive(n)
print("Received from FPGA:", received_data)


