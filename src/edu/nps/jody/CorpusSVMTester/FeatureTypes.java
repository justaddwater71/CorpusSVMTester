/**
 * 
 */
package edu.nps.jody.CorpusSVMTester;

/**
 * @author jody
 *
 */
public enum FeatureTypes 
{
	 ONE_GRAM										( 1),
	 TWO_GRAM										( 2),
	 THREE_GRAM									( 3),
	 FOUR_GRAM										( 4),
	 FIVE_GRAM										( 5),
	 GAPPY_BIGRAM									( 0),
	 GAPPY_BIGRAM_TAGGED 				( 0),
	 ORTHOGONAL_SPARSE_BIGRAM 	( 0);
	 
	 private int distance;
	 
	 FeatureTypes(int distance)
	 {
		 this.distance = distance;
	 }
	 
	 public int distance()
	 {
		 return distance;
	 }

}
