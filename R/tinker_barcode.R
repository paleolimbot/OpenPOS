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

parse.isbn(picnums)
plotbars(threshold.amp(picnums, threshold = 0.4, plot=T))


for(i in 1:nrow(digisbn)) {
  cat(paste0('digisbn.put(new BarcodePattern(new int[] {', 
             paste0(lengthsisbn[i,], collapse=", "),
             "}, ",
             ifelse(digisbn$scheme[i]=="RIGHT", "true", "false"),
             '), new BarcodeDigit("',
             digisbn$dig[i],
             '", "',
             digisbn$scheme[i],
             '")',
             ")"), 
      ";\n")
}

for(i in 1:nrow(dig1isbn)) {
  cat(paste0('dig1isbn.put("', 
             dig1isbn$code[i],
             '", new BarcodeDigit("',
             dig1isbn$dig[i],
             '", "FIRSTDIGIT")',
             ")"), 
      ";\n")
}
