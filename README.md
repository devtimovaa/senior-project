# LSB Image Steganography

A JavaFX desktop application for hiding and recovering text or images inside PNG images using Least Significant Bit (LSB) steganography. The app also includes an analysis tab for visualising and measuring how much an image has been modified.
- Embed a secret text message or a secret image inside a PNG cover image
- Extract a hidden secret from a stego PNG (automatically detected as text or image)
- Analyze a stego image with an LSB X-ray, a difference heatmap and quality metrics(MSE, PSNR, modified-pixel count)

Three selectable embedding algorithms:
  - LSB - sequential LSB replacement across the R, G, B channels
  - Randomized LSB - byte slots are shuffled with a seeded RNG; a correct integer key is needed
  - Josephus LSB 3-3-2 - pixel locations are chosen by a chaotic logistic map + Josephus elimination; each pixel stores one full byte (3 bits in R, 3 in G, 2 in B)


## How to use
### Embed tab
1. Click Choose image and select a PNG cover image
2. Pick the secret type: Text (type into the text area) or Image(pick a PNG to hide)
3. Pick an algorithm. For Randomized LSB or Josephus LSB 3-3-2, enter an integer key
4. Click Submit, choose where to save the stego PNG
### Extract tab
1. Click Choose image and select a stego PNG
2. Pick the same algorithm (and key, if used) that was used to embed
3. Click Submit. The secret is displayed as text or as an image depending on what was hidden
### Analyze tab
1. (Optional) Click Choose original image to load the pre-embedding image — needed for heatmap, MSE, and PSNR
2. Click Choose stego image to load the image to inspect
3. Click Analyze to see the LSB X-ray, difference heatmap, and statistics. Click any image to open a zoomed preview

## Project structure
The app follows the Model–View–Controller structure: views build the JavaFX layout, controllers handle user events and file I/O, and models contain the steganography and analysis logic.

## How the algorithms work
All three algorithms embed a payload of the form:
- LSB packs those bytes sequentially into the LSBs of each R, G, B channel 
- Randomized LSB writes to the same byte slots, but the slot order is different; magic bytes detects a wrong key or empty image
- Josephus LSB 3-3-2 writes one full byte per pixel using 3-3-2 bit positions

## Analysis metrics
- LSB X-ray — displays only the LSB of each channel, brightened to 0 or 255
- Difference heatmap — absolute per-channel difference between original and stego
- MSE — mean squared error per channel across all pixels -> lower = less distortion
- PSNR — peak signal-to-noise ratio in dB -> higher = less visible distortion 
