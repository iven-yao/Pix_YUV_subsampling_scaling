# pixteddy
A simple java program that does yuv subsampling and scaling.
In this program, we converting RGB into YUV, we than throw away part of YUV samples based on subsampling parameters
and reproduce the missing value by average of neighbor values to mimic subsampling.  
After subsampling, we convert YUV back to RGB, and do scaling and antialiasing.  
In antialiasing, we do an average on it's neighbors of 3x3 kernel.  

## to execute
javac ImageDisplay.java  
java ImageDisplay rgb_file Y U V Sw Sh A  
rgb_file: string to rgb file path  
Y,U,V: int to represent the subsample of YUV channels respectively. 1 suggesting no subsampling and n suggesting a sub sampling by n  
Sw,Sh: float to represent the scaling ratio of width and height respectively. should be a positive number not larger than 1.0  
A: int to represent performing antialiasing in the final result or not, 1 as true, 0 as false  

### e.g.  
1. java ImageDisplay ./test.rgb 1 1 1 1.0 1.0 0  
  No subsampling in the Y, U or V, and no scaling in w and h and no antialiasing, which
  implies that the output is the same as the input

2. java ImageDisplay ./test.rgb 1 1 1 0.5 0.5 1  
  No subsampling in Y, U or V, but the image is one fourth its original size (antialiased)
  
3. java ImageDisplay ./test.rgb 1 2 2 1.0 1.0 0  
  The output is not scaled in size, but the U and V channels are subsampled by 2. No
  subsampling in the Y channels.
  
---

## results


