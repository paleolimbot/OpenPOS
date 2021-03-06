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

pad <- function(ch) {
  nzeroes <- 4-nchar(ch)
  paste0(paste(rep("0", nzeroes), collapse=""), ch)
}

binary<-function(i) {
  a<-2^(0:9)
  b<-2*a
  pad(as.character(sapply(i,function(x) sum(10^(0:9)[(x %% b)>=a]))))
}

for(d in 0:9) {
  p <- binary(d)
  ints <- sapply(1:nchar(p), function(i) {
    ifelse(substr(p, i, i)=="1", 2, 1)
  })
  cat(paste0("digMSI.put(new BarcodePattern(new int[] {",
             paste0(ints, collapse=", "),
             '}, true), new BarcodeDigit("',
             d,
             '"));\n'))
}

c128 <- read.csv("R/code128spec.csv", colClasses = "character")


for(i in 1:(nrow(c128)-1)) {
  p <- c128$widths[i]
  ints <- sapply(1:nchar(p), function(j) {
    as.integer(substr(p, j, j))
  })
  cat(paste0("digc128.put(new BarcodePattern(new int[] {",
             paste0(ints, collapse=", "),
             '}, true), new Code128Digit(',
             c128$value[i],
             ', "',
             c128$charA[i],
             '", "',
             c128$charB[i],
             '", "',
             c128$value[i],
             '"));\n'))
}


gs1 <- read.csv("R/GS1Spec.csv", colClasses = "character")[-3]
gs1$AI_has_div <- FALSE
gs1$AI_has_div[grep("y", gs1$Code)] <- TRUE

cat(paste0(sprintf('gs1.put("%s", new AI("%s", "%s", %s, "%s", %s, %s, %s, "%s"));',
                   gs1$AI_start, gs1$Code, gs1$Description, gs1$AI_len, gs1$AI_start, 
                   gs1$mindatalen, gs1$maxdatalen, 
                   ifelse(gs1$AI_has_div, 'true', 'false'),
                   gs1$short_name), collapse="\n"))


eane <- read.csv("R/eanextraParities.csv", colClasses = "character", header=F)
eane$code <- paste0(eane$V2, eane$V3, eane$V4, eane$V5, eane$V6)
eane$code <- gsub("Odd", "A", eane$code)
eane$code <- gsub("Even", "B", eane$code)

  
cat(paste0(sprintf('digCheckEANExtra.put("%s", new BarcodeDigit("%s", "EXTRACHECK"));',
           eane$code, eane$V1),
    collapse="\n"))
