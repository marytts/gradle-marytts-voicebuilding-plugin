# Read input file
wav = Read from file... $wavFile

# Remove DC offset, if present:
Subtract mean

# First, low-pass filter the speech signal to make it more robust against noise
# (i.e., mixed noise+periodicity regions treated more likely as periodic)
sound = Filter (pass Hann band)... 0 1000 100

# determine pitch curve:
noprogress To Pitch... 0 $minPitch $maxPitch
pitch = selected()

# Write output file
Write to binary file... $pitchFile
