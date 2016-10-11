# test data and playing around

source("R/decode_barcode.R")

# # test data from phone as a batch
library(foreach)
results <- foreach(col=names(testdata), .combine=rbind) %do% {
  cbind(data.frame(col=col), parse.isbn(rev(testdata[[col]])))
}
pct <- sum(!is.na(results$isbn)) / nrow(results) * 100

nums <- rev(testdata$X1)
plotlines(scales::rescale(nums))
thr <- threshold.amp(nums, plot=T, nwindows=50)
plotbars(thr)
parse.isbn(nums)

derivprocess(nums)
parse.isbn2(nums)


parse.isbn(picnums)
plotbars(threshold.amp(picnums, threshold = 0.4, plot=T))

c39 <- read.csv("R/code39spec", colClasses = "character")

for(i in 1:nrow(c39)) {
  p <- c39$pattern[i]
  ints <- sapply(1:nchar(p), function(i) {
    ifelse(substr(p, i, i)=="W", 2, 1)
  })
  cat(paste0("digc39.put(new BarcodePattern(new int[] {",
             paste0(ints, collapse=", "),
             '}, true), new BarcodeDigit("',
             c39$sym[i],
             '", "',
             c39$checksum[i], 
             '"));\n'))
}
