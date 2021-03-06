package edu.nps.jody.CorpusSVMTester;

/**
 * This package is a collection of classes used to process a directory of text files,
 * where each file represents an author, topic, or some other classification,
 * and creates a minimum perfect hash function value based on a pre-computed
 * set of accepts words/ngrams/instances, creates SVM files, created n cross
 * validation sets for each grouping (selected by user), trains liblinear against
 * the training sets, uses liblinear to run predictions, compares predictions to 
 * "truth" in the original data, and compiles that data into a confusions matrix
 * plus other key statistics.
 @author Jody Grady, Masters Student, Naval Postgraduate School
 */