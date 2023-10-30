use std::fs::OpenOptions;
use std::io::{self, Read, Write};

const INST_DEVICE_PATH: &str = "/dev/xdma0_h2c_1";
const DATA_DEVICE_PATH: &str = "/dev/xdma0_h2c_0";
const DATA_C2H_DEVICE_PATH: &str = "/dev/xdma0_data_c2h";
const ROW_SIZE: usize = 8; // 8 elements per2 row

/* fn pack_matrix(matrix: [[u8; ROW_SIZE]; ROW_SIZE]) -> [u64; ROW_SIZE] {
  let mut packed = [0u64; ROW_SIZE];
  
  for (i, row) in matrix.iter().enumerate() {
    let mut value: u64 = 0;
    for &cell in row {
      value = (value << 8) | cell as u64;
    }
    packed[i] = value;
  }
  packed
} */

/* fn write_to_device(data: &[u64], device_path: &str) -> io::Result<()> {
  let mut device = OpenOptions::new().write(true).open(device_path)?;
  let byte_data: Vec<u8> = data.iter().flat_map(|&word| word.to_le_bytes().to_vec()).collect();
  device.write_all(&byte_data)?;
  Ok(())
} */

fn write_to_device(data: u64, device_path: &str) -> io::Result<()> {
  let mut device = OpenOptions::new().write(true).open(device_path)?;
  let byte_data: [u8; 8] = data.to_le_bytes();
  device.write_all(&byte_data)?;
  Ok(())
}

fn read_from_device(device_path: &str) -> io::Result<Vec<u64>> {
  let mut device = OpenOptions::new().read(true).open(device_path)?;
  let mut byte_data = vec![0u8; 8 * 8]; // 8 u64 values
  device.read_exact(&mut byte_data)?;

  let mut data = Vec::with_capacity(8);
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
  let instructions = [134217730u64, 34493956098u64, 4503633987110913u64, 6885369446712u64]; // Replace with actual instructions
  //write_to_device(&instructions_to_write, INST_DEVICE_PATH)?;

  for &instruction in &instructions {
    write_to_device(instruction as u64, INST_DEVICE_PATH)?;
  }

  println!("Instructions written");
  // Sample 8x8, 8-bit matrices

  // Sample 8x8 matrix of 8-bit integers
  let matrix: [[u8; 8]; 8] = [
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4], 
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4],
    [1, 2, 3, 4, 1, 2, 3, 4]
  ];

  let matrixvec = [0x0102030401020304u64, 0x0102030401020304u64, 0x0102030401020304u64, 0x0102030401020304u64, 0x0102030401020304u64, 0x0102030401020304u64, 0x0102030401020304u64, 0x0102030401020304u64]; // Replace with actual instructions

  /* for row in &matrix {
      let mut concatenated: u64 = 0;
      for &value in row {
          concatenated = (concatenated << 8) | value as u64;
      }
      // Write the concatenated 64-bit value
      write_to_device(concatenated, DATA_DEVICE_PATH)?;
      println!("Data 1 written");
  } */

  for &matrixvec in &matrixvec {
    write_to_device(matrixvec as u64, DATA_DEVICE_PATH)?;
  }

  for &matrixvec in &matrixvec {
    write_to_device(matrixvec as u64, DATA_DEVICE_PATH)?;
  }



  /* for row in &matrix {
    let mut concatenated: u64 = 0;
    for &value in row {
        concatenated = (concatenated << 8) | value as u64;
    }
    // Write the concatenated 64-bit value
    write_to_device(concatenated, DATA_DEVICE_PATH)?;
    println!("Data 2 written");
  } */

  // Read 64-bit value from device
  let read_data = read_from_device(DATA_C2H_DEVICE_PATH)?;
  println!("Read Data: {:?}", read_data);

  Ok(())
}
