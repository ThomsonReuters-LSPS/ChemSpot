package de.berlin.hu.uima.ae.tagger.abbrev;

import java.util.Iterator;
import java.util.List;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.u_compare.shared.semantic.chemical.Chemical;
import org.uimafit.util.JCasUtil;

import de.berlin.hu.chemspot.ChemSpot;
import de.berlin.hu.chemspot.Mention;
import de.berlin.hu.types.PubmedDocument;
import de.berlin.hu.util.Constants;
import de.berlin.hu.util.Constants.ChemicalType;

public class AbbreviationTagger extends JCasAnnotator_ImplBase {
	private static boolean TAG_PUBMED = true;
	private ExtractAbbrev abbrevTagger = null;
	
	public AbbreviationTagger() {
		abbrevTagger = new ExtractAbbrev();
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		String text = aJCas.getDocumentText();
		
		// get abbreviations
		List<Mention> abbreviations = abbrevTagger.getMentions(text);
		ChemSpot.printTime("ABBREV");
		
		// get PubMed documents
		Iterator<PubmedDocument> pmIterator =  JCasUtil.iterator(aJCas, PubmedDocument.class);
		PubmedDocument currDocument = TAG_PUBMED && pmIterator.hasNext() ? pmIterator.next() : null;
		
		for (Mention abbr : abbreviations) {
			if (abbr.getText().length() < 2) {
				continue;
			} 
			
			// find PubMed document that the current abbreviation belongs to
			while (TAG_PUBMED && pmIterator.hasNext() && currDocument.getEnd() < abbr.getStart()) {
				currDocument = pmIterator.next();
			}
			
			// select PubMed document or the whole text if there were none
			int offset = -1;
			if (currDocument != null) {
				text = currDocument.getCoveredText();
				offset = currDocument.getBegin();
			} else {
				text = aJCas.getDocumentText();
				offset = 0;
			}
			
			// tag all abbreviations in document
			int index = -1;
			while ((index = text.indexOf(abbr.getText(), index+1)) != -1) {
				if ((index-1 < 0 || !Character.isLetter(text.charAt(index-1)))
						&& (index + abbr.getText().length() >= text.length() || !Character.isLetter(text.charAt(index+abbr.getText().length())))) {
					int begin = offset + index;
					int end = offset + index + abbr.getText().length();
				
					createAbbreviationAnnotation(aJCas, begin, end, abbr.getCHID());
				}
			}
		}
	}
	
	private Chemical createAbbreviationAnnotation(JCas aJCas, int begin, int end, String id) {
		Chemical abbreviation = new Chemical(aJCas);
		abbreviation.setBegin(begin);
		abbreviation.setEnd(end);
        abbreviation.setId(id);
		abbreviation.setSource(Constants.ABBREV);
		abbreviation.setEntityType(ChemicalType.ABBREVIATION.toString());
		abbreviation.addToIndexes();
		return abbreviation;
	}
}
