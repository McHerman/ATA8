use std::fs::OpenOptions;
use std::io::{self, Read, Write};

const INST_DEVICE_PATH: &str = "/dev/xdma1_data_h2c";
const DATA_DEVICE_PATH: &str = "/dev/xdma0_data_h2c";
const DATA_C2H_DEVICE_PATH: &str = "/dev/xdma0_data_c2h";
const ROW_SIZE: usize = 8; // 8 elements per2 row

fn pack_matrix(matrix: [[u8; ROW_SIZE]; ROW_SIZE]) -> [u64; ROW_SIZE] {
  let mut packed = [0u64; ROW_SIZE];
  
  for (i, row) in matrix.iter().enumerate() {
    let mut value: u64 = 0;
    for &cell in row {
      value = (value << 8) | cell as u64;
    }
    packed[i] = value;
  }
  packed
}

fn write_to_device(data: &[u64], device_path: &str) -> io::Result<()> {
  let mut device = OpenOptions::new().write(true).open(device_path)?;
  let byte_data: Vec<u8> = data.iter().flat_map(|&word| word.to_le_bytes().to_vec()).collect();
  device.write_all(&byte_data)?;
  Ok(())
}

fn read_from_device(device_path: &str) -> io::Result<Vec<u64>> {
  let mut device = OpenOptions::new().read(true).open(device_path)?;
  let mut byte_data = vec![0u8; ROW_SIZE * 8];
  device.read_exact(&mut byte_data)?;
  let mut data = Vec::with_capacity(ROW_SIZE);
  
  for chunk in byte_data.chunks_exact(8) {
    data.push(u64::from_le_bytes([
      chunk[0], chunk[1], chunk[2], chunk[3],
      chunk[4], chunk[5], chunk[6], chunk[7]
    ]));
  }
  Ok(data)
}

fn main() -> io::Result<()> {
  // Sample instructions for writing
  let instructions_to_write = [1125899906842626u64, 1125899915814914u64, 5764607528476673u64, 1125899916843523u64]; // Replace with actual instructions
  write_to_device(&instructions_to_write, INST_DEVICE_PATH)?;

  println!("Instructions written");
  // Sample 8x8, 8-bit matrices

  let matrix1 = [
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
  ];

  let matrix2 = [
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
  ];

  let packed_matrix1 = pack_matrix(matrix1);
  let packed_matrix2 = pack_matrix(matrix2);
  let mut all_data = Vec::new();

  all_data.extend_from_slice(&packed_matrix1);
  all_data.extend_from_slice(&packed_matrix2);

  write_to_device(&all_data, DATA_DEVICE_PATH)?;

  println!("Data written");

  let read_data = read_from_device(DATA_C2H_DEVICE_PATH)?;

  println!("Read Data: {:?}", read_data);

  Ok(())
}
