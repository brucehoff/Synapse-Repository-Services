#!/usr/bin/env Rscript

#----- Load the Synpase R client
library(synapseClient)

#----- Unpack our command line parameters
inputLayerId <- getInputLayerIdArg()
inputDatasetId <- getInputDatasetIdArg()

#----- Log into Synapse
synapseLogin(getUsernameArg(), getPasswordArg())

#----- Decide whether this script wants to work on this input layer
dataset <- getDataset(id=inputDatasetId)
if('coad' != dataset$name) {
  skipWorkflowTask('this script only handles TCGA colon cancer data')
}

inputLayer <- getLayer(id=inputLayerId)
if('E' != inputLayer$type) {
  skipWorkflowTask('this script only handles expression data')
}

layerAnnotations <- getAnnotations(inputLayer)
if('Level_2' != layerAnnotations$stringAnnotations$format) {
	skipWorkflowTask('this script ony handles level 2 expression data from TCGA')
}

#----- Download, unpack, and load the expression layer
expressionDataFiles <- synapseClient:::.cacheFiles(entity=inputLayer)
# TODO load each of the files into R objects

#----- Download, unpack, and load the clinical layer of this TCGA dataset  
#      because we need it as additional input to this script
datasetLayers <- getLayers(entity=dataset)
clinicalLayer <- datasetLayers$C
clinicalDataFiles <- synapseClient:::.cacheFiles(entity=clinicalLayer)
clinicalData <- read.table(clinicalDataFiles[[4]], sep='\t')

#----- Do interesting work with the clinical and expression data R objects
#      e.g., make a matrix by combining expression and clinical data
outputData <- t(clinicalData)

#----- Now we have an analysis result, add the metadata for the new layer 
#      to Synapse and upload the analysis result
outputLayer <- list()
outputLayer$parentId <- inputDatasetId
outputLayer$name <- paste(dataset$name, inputLayer$name, clinicalLayer$name, sep='-')
outputLayer$type <- 'E'

storedOutputLayer <- storeLayerData(layerMetadata=outputLayer, layerData=outputData)

#----- Add some annotations to our newly stored output layer
outputLayerAnnotations <- getAnnotations(storedOutputLayer)
outputLayerAnnotations$stringAnnotations$format <- 'sageMatrix'
storedOutputLayerAnnotations <- updateAnnotations(outputLayerAnnotations)

finishWorkflowTask(outputLayerId=storedOutputLayer$id)

