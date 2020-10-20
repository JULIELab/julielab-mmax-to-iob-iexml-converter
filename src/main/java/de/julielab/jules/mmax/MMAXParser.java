package de.julielab.jules.mmax;

import org.apache.xerces.dom.DocumentImpl;
import org.eml.MMAX2.annotation.markables.Markable;
import org.eml.MMAX2.annotation.markables.MarkableLevel;
import org.eml.MMAX2.discourse.MMAX2Discourse;
import org.eml.MMAX2.discourse.MMAX2DiscourseElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MMAXParser {
    private static final String TAG_PUBMED_ARTICLE = "PubmedArticle";
    private static final String TAG_MEDLINE_CITATION = "MedlineCitation";
    private static final String TAG_PMID = "PMID";
    private static final String TAG_ARTICLE = "Article";
    private static final String TAG_ARTICLE_TITLE = "ArticleTitle";
    private static final String TAG_ABSTRACT = "Abstract";
    private static final String TAG_ABSTRACT_TEXT = "AbstractText";
    private static final String ATTRIBUTE_STATUS = "Status";
    private static final String ATTRIBUTE_OWNER = "Owner";
    private static final String ATTR_VALUE_MEDLINE = "MEDLINE";
    private static final String ATTR_VALUE_NLM = "NLM";
    private static final String SEM_TYP_PRGE = "PRGE";
    private final static Logger logger = LoggerFactory.getLogger(MMAXParser.class);
    private HashMap<String, Integer> prioMap;
    private String ORIGINAL_INPUT_DIR = "";
    private String CONFIG_PRIO_LIST;
    private String OUTPUT_DIR;
    private String[] TYPES = new String[0];
    private String actualPath;
    private String OUTPUT_FORMAT;
    private String INPUT_DIR;

    private Queue<File> folderList;

    public static void main(String[] args) {

        if (args.length < 6) {
            System.out.println("Usage:");
            System.out.println("param 1: mmax base dir (contains mmax project folders)");
            System.out.println("param 2: output dir for parsed files");
            System.out.println("param 3: path to prio list");
            System.out.println("param 4: output format (IOB or IEXML)");
            System.out.println("param 5: path to original text files (optional Parameter)");
            System.out.println("param 6..x: annotation level name, e.g. 'proteins'");
            System.exit(1);
        }

        MMAXParser parser = new MMAXParser();
        //
        // init constants
        parser.INPUT_DIR = args[0];
        parser.OUTPUT_DIR = args[1];
        parser.CONFIG_PRIO_LIST = args[2];
        parser.OUTPUT_FORMAT = args[3];
        parser.ORIGINAL_INPUT_DIR = args[4];

        String[] types = new String[args.length - 5];
        for (int i = 0; i < types.length; i++) {
            types[i] = args[i + 5];
        }
        parser.TYPES = types;
        parser.initialize();

        while (parser.hasNext()) {
            parser.parseNextFolder();
        }
        logger.info(Statistics.getStatistitcs());
    }

    private void setUpPrioMap(String conf) {
        try {
            this.prioMap = new HashMap<String, Integer>();
            FileInputStream fstream = new FileInputStream(conf);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            // Read File Line By Line
            int count = 0;
            while ((strLine = br.readLine()) != null) {
                // Print the content on the console
                prioMap.put(strLine, count);
                count++;
            }
        } catch (IOException e) {
            error("Error while parsing " + conf);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private String getPMID() {
        try {
            FileInputStream fstream = new FileInputStream(this.actualPath + "Basedata.uri");
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            // Read File Line By Line
            int count = 0;
            String pmid = "";
            while ((strLine = br.readLine()) != null) {
                count++;
                pmid = strLine;
            }
            if (count > 1) {
                error("unknown data in " + actualPath + "Basedata.uri");
                System.exit(1);
                return null;
            }
            return pmid;
        } catch (IOException e) {
            error("Error while parsing " + this.actualPath + "Basedata.uri");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public boolean hasNext() {
        return !this.folderList.isEmpty();

    }

    public void initialize() {
        this.folderList = new LinkedList<>();
        setUpFolderList();
        setUpPrioMap(this.CONFIG_PRIO_LIST);
    }

    private void debug(String i) {
        logger.debug(i);
    }

    private void error(String e) {
        logger.error(e);
    }

    public void processCas() {
        Statistics.projects++;
        actualPath = this.folderList.poll().getAbsolutePath() + "/";
        // rename style file from default_style.xsl to generic_nongui_style.xsl
        // (necessary for api use)
        File style = new File(actualPath + "Styles/default_style.xsl");
        style.renameTo(new File(actualPath + "Styles/generic_nongui_style.xsl"));

        File mmaxfile = new File(actualPath + "project.mmax");
        MMAX2Discourse discourse = MMAX2Discourse.buildDiscourse(mmaxfile.getAbsolutePath());

        // text aus basedata mit space dazwischen
        String documentText = discourse.getNextDocumentChunk();

        WordInformation[] words = new WordInformation[discourse.getDiscourseElementCount()];

        int textPosition = 0;
        // Words aus basedata
        for (MMAX2DiscourseElement elem : discourse.getDiscourseElements()) {
            WordInformation word = new WordInformation();
            word.setId(elem.getID());
            int discoursePosition = elem.getDiscoursePosition();
            word.setPosition(discoursePosition);
            StringBuilder textBuilder = new StringBuilder();
            int end = discourse.getDisplayEndPositionFromDiscoursePosition(discoursePosition);
            for (textPosition = discourse.getDisplayStartPositionFromDiscoursePosition(discoursePosition); textPosition <= end; textPosition++) {
                textBuilder.append(documentText.charAt(textPosition));
            }
            word.setText(textBuilder.toString());
            words[discoursePosition] = word;
            // System.out.println(word);
        }

        // handle sentences
        handleSentences(discourse, words, "sentence");

        // handle entity annotations types
        for (String levelName : TYPES) {
            handleMarkableType(discourse, words, levelName);
        }

        this.clearTypes(words, discourse);

        this.produceOutput(words);

        // set stylefile back to normal
        style = new File(actualPath + "Styles/generic_nongui_style.xsl");
        style.renameTo(new File(actualPath + "Styles/default_style.xsl"));

        Statistics.projects++;
    }

    public void parseNextFolder() {
        this.processCas();
    }

    private void produceOutput(WordInformation[] words) {
        if (this.OUTPUT_FORMAT.equals("IEXML")) {
            this.produceIEXMLOutput(words);
        } else if (this.OUTPUT_FORMAT.equals("IOB")) {
            this.produceIOBOutput(words);
        } else {
            error("unkonw output format " + OUTPUT_FORMAT);
            System.exit(1);
        }
    }

    private void produceIEXMLOutput(WordInformation[] words) {
        StringBuilder out = new StringBuilder();
        StringBuilder outPlain = new StringBuilder();
        String pmid = this.getPMID();
        if (this.ORIGINAL_INPUT_DIR != null && this.ORIGINAL_INPUT_DIR.length() > 0)
            this.handleOriginalTextInformation(pmid, words);

        int sentenceCount = 0;
        int lastSentenceBegin = 0;
        for (int i = 0; i < words.length; i++) {
            this.checkSentence(words, i);
            WordInformation word = words[i];
            if (lastSentenceBegin != word.getSentence().getBegin()) {
                sentenceCount++;
                lastSentenceBegin = word.getSentence().getBegin();
            }
            int position = word.getPosition();
            if (lastSentenceBegin == position) {
                out.append("<s id=\"" + sentenceCount + "\">");
                outPlain.append("<s id=\"" + sentenceCount + "\">");
            }
            MarkableContainer m = null;
            for (MarkableContainer mL : word.getMarkables()) {
                if (!mL.isIgnore()) {
                    if (m != null) {
                        error("to many Types at one word[id=" + word.getId() + "] remaining at " + actualPath);
                        System.exit(1);
                    }
                    m = mL;
                }
            }
            if (m != null && m.getBegin() == position) {
                out.append("<e id=\":::").append(SEM_TYP_PRGE).append("\" sub=\"").append(SEM_TYP_PRGE).append(":").append(m.getLable()).append("\">");
            }
            out.append(word.getText());
            outPlain.append(word.getText());
            if (m != null && m.getEnd() == position) {
                out.append("</e>");
            }
            if (position == word.getSentence().getEnd()) {
                out.append("</s>");
                outPlain.append("</s>");
            }
            if (word.isFollowedBySpace()) {
                out.append(" ");
                outPlain.append(" ");
            }
        }
        String text = out.toString().replaceAll("[\\s]+", " ");
        String textPlain = outPlain.toString().replaceAll("[\\s]+", " ");

        try {
            File entities = new File(OUTPUT_DIR + "/entities/");
            if (!entities.exists()) {
                entities.mkdir();
            }

            File plain = new File(OUTPUT_DIR + "/plain/");
            if (!plain.exists()) {
                plain.mkdir();
            }
            writeStringToFile(getIEXMLDocAsString(text, pmid), new File(OUTPUT_DIR + "/entities/", pmid), true);
            writeStringToFile(getIEXMLDocAsString(textPlain, pmid), new File(OUTPUT_DIR + "/plain/", pmid), true);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void handleOriginalTextInformation(String pmid, WordInformation[] words) {
        if (this.ORIGINAL_INPUT_DIR.length() > 0 && !this.ORIGINAL_INPUT_DIR.endsWith("/"))
            this.ORIGINAL_INPUT_DIR += "/";

        File file = new File(ORIGINAL_INPUT_DIR + pmid);
        if (!file.exists()) {
            warn("no original File found for " + pmid + " using only mmax text.");
            return;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            int wordCounter = 0;
            int i;
            try {
                WordInformation actualWord = words[wordCounter];
                String actualText = actualWord.getText();
                actualWord.setFollowedBySpace(false);
                int wordCharCounter = 0;
                while ((i = isr.read()) >= 0) {
                    if (wordCharCounter >= actualText.length()) {
                        wordCounter++;
                        if (wordCounter < words.length) {
                            actualWord = words[wordCounter];
                            actualText = actualWord.getText();
                            actualWord.setFollowedBySpace(false);
                            wordCharCounter = 0;
                        } else {
                            if (!Character.isWhitespace(i)) {
                                this.warn("original Text contains more words than mmax information");
                            }
                            return;
                        }
                    }

                    if (actualText.charAt(wordCharCounter) == i) {
                        wordCharCounter++;
                    } else {
                        if (!Character.isWhitespace(i)) {
                            this.warn("there is a non whitespace character different in original text at document " + pmid + " critical character is '" + i + "' near word '" + actualText + "'");
                        } else {
                            words[wordCounter - 1].setFollowedBySpace(true);
                        }
                    }
                }
                isr.close();
            } catch (IOException e) {
                System.err.println("Fehler beim Lesen: " + e);
            }
        } catch (Exception e) {
            logger.error("", e);
            System.exit(1);
        }
    }

    private String getIEXMLDocAsString(String text, String pmid) throws ParserConfigurationException, TransformerException {
        // create xml document
        Document doc = new DocumentImpl();
        // impl.createDocument(null,null,null);
        Element pubMedAbstractEl = doc.createElement(TAG_PUBMED_ARTICLE);
        Element medlineCitationEl = doc.createElement(TAG_MEDLINE_CITATION);
        Element pmidEl = doc.createElement(TAG_PMID);
        Element articleEl = doc.createElement(TAG_ARTICLE);
        Element titleEl = doc.createElement(TAG_ARTICLE_TITLE);
        Element abstrEl = doc.createElement(TAG_ABSTRACT);
        Element abstrTextEl = doc.createElement(TAG_ABSTRACT_TEXT);

        doc.appendChild(pubMedAbstractEl);

        pubMedAbstractEl.appendChild(medlineCitationEl);

        medlineCitationEl.appendChild(pmidEl);
        medlineCitationEl.appendChild(articleEl);
        medlineCitationEl.setAttribute(ATTRIBUTE_STATUS, ATTR_VALUE_MEDLINE);
        medlineCitationEl.setAttribute(ATTRIBUTE_OWNER, ATTR_VALUE_NLM);

        articleEl.appendChild(titleEl);
        articleEl.appendChild(abstrEl);

        abstrEl.appendChild(abstrTextEl);
        abstrTextEl.setTextContent(text);
        pmidEl.setTextContent(pmid);
        titleEl.setTextContent("");

        // transform the Document into a String
        DOMSource domSource = new DOMSource(doc);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        // transformer.setOutputProperty (OutputKeys.OMIT_XML_DECLARATION,
        // "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        // transformer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult sr = new StreamResult(sw);
        transformer.transform(domSource, sr);
        // Formatter escapes all "<" and ">" in AbstractText. Thus 'unescape'
        // them again...
        String xml = sw.toString();
        xml = xml.replaceAll("&lt;/s&gt;", "</s>");
        xml = xml.replaceAll("&lt;/e&gt;", "</e>");
        xml = xml.replaceAll("&lt;s id=", "<s id=");
        xml = xml.replaceAll("&lt;e id=", "<e id=");
        xml = xml.replaceAll("&lt;s id=", "<s id=");
        xml = xml.replaceAll("&lt;e id=", "<e id=");
        xml = xml.replaceAll("\"&gt;", "\">");
        return xml;
    }

    public boolean writeStringToFile(String string, File output, boolean overwrite) {
        if (!overwrite && output.exists()) {
            error("could not create file since it already exists: " + output.getPath());
        } else {
            try {
                if (!output.exists()) {
                    output.createNewFile();
                }
                BufferedWriter bw = new BufferedWriter(new FileWriter(output));
                bw.write(string);
                bw.close();
                debug("wrote file " + output.getPath());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void produceIOBOutput(WordInformation[] words) {
        File file = new File(this.OUTPUT_DIR + "/" + this.getPMID() + ".iob");
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            this.checkSentence(words, i);
            WordInformation word = words[i];
            MarkableContainer m = null;
            for (MarkableContainer mL : word.getMarkables()) {
                if (!mL.isIgnore()) {
                    if (m != null) {
                        error("to many Types at one word[id=" + word.getId() + "] remaining at " + actualPath);
                        System.exit(1);
                    }
                    m = mL;
                }
            }
            out.append(word.getText()).append('\t');
            if (m == null) {
                out.append('O').append('\n');
            } else if (m.getBegin() == word.getPosition()) {
                out.append("B-").append(m.getLable()).append('\n');
            } else {
                out.append("I-").append(m.getLable()).append('\n');
            }
            if (word.getSentence().getEnd() == word.getPosition()) {
                out.append('\n');
            }
        }
        writeStringToFile(out.toString(), file, true);

    }

    private void clearTypes(WordInformation[] words, MMAX2Discourse discourse) {
        for (int i = 0; i < words.length; i++) {
            if (words[i].getMarkables().size() > 1) {
                this.handleMultipleMarkables(words[i].getMarkables());
            }
        }
    }

    private void handleMultipleMarkables(List<MarkableContainer> markables) {
        MarkableContainer bestContainer = null;
        for (MarkableContainer m : markables) {
            if (bestContainer == null) {
                bestContainer = m;
            } else {
                if (m.getBegin() < bestContainer.getBegin()) {
                    bestContainer.setIgnore(true);
                    bestContainer = m;
                } else if (bestContainer.getBegin() == m.getBegin()) {
                    if (bestContainer.getEnd() < m.getEnd()) {
                        bestContainer.setIgnore(true);
                        bestContainer = m;
                    } else if (bestContainer.getEnd() == m.getEnd()) {
                        if (bestContainer.getPriority() < m.getPriority()) {
                            bestContainer.setIgnore(true);
                            bestContainer = m;
                        } else {
                            if (bestContainer.getPriority() == m.getPriority() && !bestContainer.getLable().equals(m.getLable())) {
                                error("found overlapping markables with same range and same priority but different lable (markable[id=" + bestContainer.getId() + "] and markable[id=" + m.getId() + "]) in " + actualPath);
                                System.exit(1);
                            }
                            m.setIgnore(true);
                        }
                    } else {
                        m.setIgnore(true);
                    }
                } else {
                    m.setIgnore(true);
                }
            }
        }
    }

    private void handleSentences(MMAX2Discourse discourse, WordInformation[] words, String levelName) {
        MarkableLevel level = discourse.getMarkableLevelByName(levelName, false);
        if (level.getMarkables() == null || level.getMarkables().isEmpty()) {
            error("no senteces in project " + this.actualPath);
            System.exit(1);
        }
        for (Object markable : level.getMarkables()) {
            Markable markable2 = (Markable) markable;
            if (!markable2.isDiscontinuous()) {
                Statistics.sentences++;
                // create container for markable
                MarkableContainer c = new MarkableContainer();
                c.setBegin(markable2.getLeftmostDiscoursePosition());
                c.setEnd(markable2.getRightmostDiscoursePosition());
                c.setId(markable2.getID());
                c.setLable(markable2.getAttributeValue(levelName));

                // add container to every word in range
                for (int i = c.getBegin(); i <= c.getEnd(); i++) {
                    if (words[i].getSentence() != null) {
                        this.error("Overlapping sentences in " + actualPath + " (sentence[id=" + c.getId() + "] and sentence[id=" + words[i].getSentence().getId() + "]");
                        System.exit(1);
                    }
                    words[i].setSentence(c);
                }

            } else {
                this.error("Discontinous sentence in " + actualPath);
                System.exit(1);
            }
        }
    }

    private void handleMarkableType(MMAX2Discourse discourse, WordInformation[] words, String levelName) {
        MarkableLevel level = discourse.getMarkableLevelByName(levelName, false);
        if (level.getMarkables() == null || level.getMarkables().isEmpty()) {
            warn("no markables for level " + levelName);
        }
        markLoop:
        for (Object markable : level.getMarkables()) {
            Markable markable2 = (Markable) markable;
            if (!markable2.isDiscontinuous()) {
                // create container for markable
                MarkableContainer c = new MarkableContainer();
                c.setBegin(markable2.getLeftmostDiscoursePosition());
                c.setEnd(markable2.getRightmostDiscoursePosition());
                c.setId(markable2.getID());
                c.setLable(markable2.getAttributeValue(levelName));
                Integer priority = this.prioMap.get(c.getLable());
                if (priority != null) {
                    c.setPriority(priority);
                } else {
                    this.error("no priority defined for lable " + c.getLable() + " using minimum prority");
                    c.setPriority(prioMap.size());
                }
                this.checkSentence(words, c.getBegin());
                int sentenceBegin = words[c.getBegin()].getSentence().getBegin();

                // add container to every word in range
                for (int i = c.getBegin(); i <= c.getEnd(); i++) {
                    if (words[i].getSentence().getBegin() != sentenceBegin) {
                        this.warn("Markable[id=" + c.getId() + ", level=" + levelName + "] spreads over multiple sentences... skip");
                        c.setIgnore(true);
                        continue markLoop;
                    } else {
                        words[i].addMarkable(c);
                    }
                }
                Statistics.addType(c);
            } else {
                Statistics.segmentet_Types++;
            }
        }
    }

    private void checkSentence(WordInformation[] words, int i) {
        if (words[i].getSentence() == null) {
            if (words[i].getId().contains(".")) {
                // if the token is splitted, put to nextSentence...
                this.warn("splitted token without sentence found at " + actualPath + " word[id=" + words[i].getId() + "]");
                MarkableContainer findNextSentence = this.findNextSentence(words, i);
                if (findNextSentence != null) {
                    words[i].setSentence(findNextSentence);
                } else {
                    this.error("error in " + actualPath + ": missing Sentence for word[id=" + words[i].getId() + "]");
                    System.exit(1);
                }
            } else {
                this.error("error in " + actualPath + ": missing Sentence for word[id=" + words[i].getId() + "]");
                System.exit(1);
            }
        }
    }

    private MarkableContainer findNextSentence(WordInformation[] words, int i) {
        for (int j = i; j < words.length; j++) {
            if (words[j].getSentence() != null)
                return words[j].getSentence();
        }
        return null;
    }

    private void warn(String string) {
        logger.warn(string);
    }

    private void setUpFolderList() {
        if (!this.INPUT_DIR.endsWith("/"))
            this.INPUT_DIR += "/";

        File rootX = new File(this.INPUT_DIR);

        if (!rootX.exists()) {
            File dir1 = new File(".");
            try {
                rootX = new File(dir1.getCanonicalPath() + this.INPUT_DIR);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            if (!rootX.exists()) {
                error(INPUT_DIR + " does not exist");
                System.exit(1);
            }
        }

        for (String rootFolder : rootX.list()) {
            if (!rootFolder.endsWith("/"))
                rootFolder += "/";
            File root = new File(this.INPUT_DIR + rootFolder);
            if (root.isDirectory()) {
                this.folderList.add(root);
            }
        }
    }
}
