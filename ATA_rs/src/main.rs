use std::fs::OpenOptions;
use std::io::{self, Read, Write};

const H2C_DEVICE_PATH: &str = "/dev/xdma0_h2c_0";
const C2H_DEVICE_PATH: &str = "/dev/xdma0_c2h_0";
const DATA_SIZE: usize = 128; // Change this as required

fn write_to_device(data: &[u64]) -> io::Result<()> {
    let mut h2c_device = OpenOptions::new().write(true).open(H2C_DEVICE_PATH)?;
    
    // Convert the u64 data to u8 for writing
    let byte_data: Vec<u8> = data.iter().flat_map(|&word| word.to_le_bytes().to_vec()).collect();

    h2c_device.write_all(&byte_data)?;
    Ok(())
}

fn read_from_device() -> io::Result<Vec<u64>> {
    let mut c2h_device = OpenOptions::new().read(true).open(C2H_DEVICE_PATH)?;
    let mut byte_data = vec![0u8; DATA_SIZE * 8]; // For 64-bit data, hence * 8

    c2h_device.read_exact(&mut byte_data)?;

    // Convert the received u8 data back to u64
    let mut data = Vec::with_capacity(DATA_SIZE);
    for chunk in byte_data.chunks_exact(8) {
        data.push(u64::from_le_bytes([
            chunk[0], chunk[1], chunk[2], chunk[3], 
            chunk[4], chunk[5], chunk[6], chunk[7]
        ]));
    }

    Ok(data)
}

fn main() -> io::Result<()> {
    // Sample data for writing
    let data_to_write = (1u64..=DATA_SIZE as u64).collect::<Vec<_>>();
    
    println!("penis");

    write_to_device(&data_to_write)?;

    println!("write completed");    

    let read_data = read_from_device()?;
    println!("Read Data: {:?}", read_data);

    Ok(())
}
