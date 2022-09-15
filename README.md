# pixteddy
A simple java program that does yuv subsampling and scaling.

to execute:
javac ImageDisplay.java
java ImageDisplay rgb_file Y U V Sw Sh A
rgb_file: string to rgb file path
Y,U,V: int to represent the subsample of YUV channels respectively. 1 suggesting no subsampling and n suggesting a sub sampling by n
Sw,Sh: float to represent the scaling ratio of width and height respectively. should be a positive number not larger than 1.0
A: int to represent performing antialiasing in the final result or not, 1 as true, 0 as false

e.g.
java ImageDisplay ./test.rgb 1 1 1 1.0 1.0 0
  No subsampling in the Y, U or V, and no scaling in w and h and no antialiasing, which
  implies that the output is the same as the input

java ImageDisplay ./test.rgb 1 1 1 0.5 0.5 1
  No subsampling in Y, U or V, but the image is one fourth its original size (antialiased)
  
java ImageDisplay ./test.rgb 1 2 2 1.0 1.0 0
  The output is not scaled in size, but the U and V channels are subsampled by 2. No
  subsampling in the Y channels.
  
##########################################################################################

In this program, we converting RGB into YUV, we than throw away part of YUV samples based on subsampling parameters
and reproduce the missing value by average of neighbor values to mimic subsampling.
After subsampling, we convert YUV back to RGB, and do scaling and antialiasing.
In antialiasing, we do an average on it's neighbors of 3x3 kernel.

here's the conversion matrix multiplication used in this program:
RGB to YUV:</br>
|Y|   |0.299  0.587  0.114| |R|

|U| = |0.596 -0.274 -0.322| |G|

|V|   |0.211 -0.523  0.312| |B|

YUV to RGB:
|R|   |1.000  0.956  0.621| |Y|

|G| = |1.000 -0.272 -0.647| |U|

|B|   |1.000 -1.106  1.703| |V|
