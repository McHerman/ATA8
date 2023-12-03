use std::fs::File;
use std::io::{self, BufRead, BufReader, Write};
use byteorder::{BigEndian, ReadBytesExt, WriteBytesExt};
use std::time::Instant;

const N: usize = 10; // Number of outer iterations
const M: usize = 16; // Number of inner iterations

fn write_to_device(file: &mut File, data: &[u8]) -> io::Result<()> {
    file.write_all(data)?;
    Ok(())
}

fn read_from_device(file: &mut File) -> io::Result<Vec<u64>> {
    let mut data = vec![0u64; 32]; // Adjust the size based on your requirements
    for value in &mut data {
        *value = file.read_u64::<BigEndian>()?;
    }
    Ok(data)
}

fn main() -> io::Result<()> {
    let mut inst_write = File::create("/dev/xdma0_h2c_1")?;
    let mut data_write = File::create("/dev/xdma0_h2c_0")?;
    let mut fd_c2h = File::open("/dev/xdma0_c2h_0")?;

    let file = File::open("/home/karlhk/PCIE/ATA8/rustTestProgram_Output.txt")?;
    let reader = BufReader::new(file);
    let lines = reader.lines().collect::<Result<Vec<_>, io::Error>>()?;

    let total_start_time = Instant::now();

    for _ in 0..N {
        for iteration in 0..M {
            let start_time = Instant::now();

            let start_index = iteration * 4;
            let instructions = lines[start_index..start_index + 4]
                .iter()
                .map(|line| u64::from_str_radix(line, 2).expect("Failed to parse instruction"))
                .collect::<Vec<_>>();

            let mut buffer = Vec::new();
            for &inst in &instructions {
                buffer.write_u64::<BigEndian>(inst)?;
            }

            write_to_device(&mut inst_write, &buffer)?;

            // Pack and write matrix data
            let matrix_data = vec![0x0102030401020304u64; 32];
            let mut matrix_buffer = Vec::new();
            for &data in &matrix_data {
                matrix_buffer.write_u64::<BigEndian>(data)?;
            }
            write_to_device(&mut data_write, &matrix_buffer)?;
            write_to_device(&mut data_write, &matrix_buffer)?;

            // Read and log data
            let read_data = read_from_device(&mut fd_c2h)?;
            let end_time = Instant::now();

            println!("Iteration {}: {:?}", iteration + 1, read_data);
            println!("Time for iteration: {:?}", end_time.duration_since(start_time));
        }
    }

    let total_end_time = Instant::now();
    println!("Total execution time: {:?}", total_end_time.duration_since(total_start_time));
    println!("Average time per iteration: {:?}", total_end_time.duration_since(total_start_time) / (N * M) as u32);

    Ok(())

}
