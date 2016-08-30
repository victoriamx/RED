/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.FilePosition;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.ARobotSectionTable;
import org.rf.ide.core.testdata.text.read.EndOfLineBuilder;
import org.rf.ide.core.testdata.text.read.EndOfLineBuilder.EndOfLineTypes;
import org.rf.ide.core.testdata.text.read.IRobotLineElement;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.LineReader.Constant;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.separators.Separator;
import org.rf.ide.core.testdata.text.write.SectionBuilder.Section;

import com.google.common.base.Optional;

public class DumperHelper {

    private static final int NUMBER_OF_AFTER_UNIT_ELEMENTS_TO_TREAT_AS_NEW_UNIT_START = 3;

    // private static final int MAX_NUMBER_OF_COLUMN_IN_LINE = 7;
    //
    // private static final int MAX_NUMBER_OF_CHARS_IN_LINE = 120;

    private static final String EMPTY = "\\";

    private final ARobotFileDumper currentDumper;

    private final DumpLineUpdater aDumpLineUpdater;

    private final EmptyLineDumper anEmptyLineDumper;

    private final HeaderDumperHelper aHeaderDumper;

    private final SeparatorsDumpHelper aSeparatorDumper;

    private final NotModelRelatedHashCommentedLineDumper aCommentHashDumper;

    public DumperHelper(final ARobotFileDumper currentDumper) {
        this.currentDumper = currentDumper;
        this.aDumpLineUpdater = new DumpLineUpdater(this);
        this.anEmptyLineDumper = new EmptyLineDumper(this);
        this.aHeaderDumper = new HeaderDumperHelper(this);
        this.aSeparatorDumper = new SeparatorsDumpHelper(this);
        this.aCommentHashDumper = new NotModelRelatedHashCommentedLineDumper(this);
    }

    public DumpLineUpdater getDumpLineUpdater() {
        return this.aDumpLineUpdater;
    }

    public EmptyLineDumper getEmptyLineDumper() {
        return this.anEmptyLineDumper;
    }

    public HeaderDumperHelper getHeaderDumpHelper() {
        return this.aHeaderDumper;
    }

    public SeparatorsDumpHelper getSeparatorDumpHelper() {
        return this.aSeparatorDumper;
    }

    public NotModelRelatedHashCommentedLineDumper getHashCommentDumper() {
        return this.aCommentHashDumper;
    }

    public boolean isCurrentFileDirty() {
        return this.currentDumper.isFileDirty();
    }

    public void addEOFinCaseIsMissing(final RobotFile model, final List<RobotLine> lines) {
        IRobotLineElement buildEOL = new EndOfLineBuilder()
                .setEndOfLines(Arrays.asList(new Constant[] { Constant.EOF })).buildEOL();

        if (lines.isEmpty()) {
            getDumpLineUpdater().updateLine(model, lines, buildEOL);
        } else {
            RobotLine robotLine = lines.get(lines.size() - 1);
            if (robotLine.getEndOfLine() == null || robotLine.getEndOfLine().getFilePosition().isNotSet()) {
                getDumpLineUpdater().updateLine(model, lines, buildEOL);
            }
        }
    }

    public Separator getSeparator(final RobotFile model, final List<RobotLine> lines, final IRobotLineElement lastToken,
            final IRobotLineElement currentToken) {
        return currentDumper.getSeparator(model, lines, lastToken, currentToken);
    }

    public boolean isEndOfLine(final IRobotLineElement elem) {
        boolean result = false;
        for (final IRobotTokenType t : elem.getTypes()) {
            if (t instanceof EndOfLineTypes) {
                result = true;
                break;
            }
        }

        return result;
    }

    public IRobotLineElement getLineSeparator(final RobotFile model) {
        String eol = model.getParent().getFileLineSeparator();
        if (eol == null || eol.isEmpty()) {
            eol = System.lineSeparator();
        }

        RobotToken tempEOL = new RobotToken();
        tempEOL.setRaw(eol);
        tempEOL.setText(eol);

        return EndOfLineBuilder.newInstance().setEndOfLines(Constant.get(tempEOL)).buildEOL();
    }

    public IRobotLineElement getLineSeparator(final RobotFile model, final FilePosition fPosEnd) {
        IRobotLineElement eol = null;
        final List<RobotLine> fileContent = model.getFileContent();
        if (!fileContent.isEmpty() && fPosEnd.getLine() != FilePosition.NOT_SET) {
            final RobotLine robotLine = fileContent.get(fPosEnd.getLine() - 1);
            eol = robotLine.getEndOfLine();
        }

        if (eol == null) {
            eol = getLineSeparator(model);
        }

        return eol;
    }

    public int getLastSortedToDump(final RobotFile model, final List<Section> sections,
            final List<AModelElement<ARobotSectionTable>> sortedElements) {
        final int size = sortedElements.size();
        int index = size - 1;
        int nextFound = 0;
        int nextStartFoundIndex = -1;

        if (sections.size() >= 1) {
            final Section currentSection = sections.get(0);
            final Set<Integer> startPosForElements = new HashSet<>();
            final List<Section> subElements = currentSection.getSubSections();
            for (final Section elem : subElements) {
                startPosForElements.add(elem.getStart().getOffset());
            }

            final Set<Integer> nextStartPosForElements = new HashSet<>();
            if (sections.size() > 1) {
                final Section nextSection = sections.get(1);
                final List<Section> nextElements = nextSection.getSubSections();
                for (final Section elem : nextElements) {
                    nextStartPosForElements.add(elem.getStart().getOffset());
                }
            }

            for (int elemIndex = 0; elemIndex < size; elemIndex++) {
                final AModelElement<ARobotSectionTable> e = sortedElements.get(elemIndex);
                FilePosition pos = e.getBeginPosition();
                if (pos.isNotSet()) {
                    if (size == index || elemIndex - 1 == index) {
                        index = elemIndex;
                        nextFound = 0;
                        nextStartFoundIndex = -1;
                    }

                } else if (startPosForElements.contains(pos.getOffset())
                        || containsOneOfElementOffset(e, startPosForElements)) {
                    index = elemIndex;
                    nextFound = 0;
                    nextStartFoundIndex = -1;
                } else {
                    if (nextStartPosForElements.contains(pos.getOffset())
                            || containsOneOfElementOffset(e, nextStartPosForElements)) {
                        if (nextStartFoundIndex == -1) {
                            nextStartFoundIndex = elemIndex;
                            nextFound++;
                        } else if (nextFound == getNumberOfNewToTreatAsNewUnit()) {
                            index = nextStartFoundIndex;
                            break;
                        }
                    } else {
                        Optional<Integer> line = model.getRobotLineIndexBy(pos.getOffset());
                        if (line.isPresent()) {
                            final int lineIndex = line.get();
                            final RobotLine robotLine = model.getFileContent().get(lineIndex);
                            final Optional<Integer> elementPositionInLine = robotLine
                                    .getElementPositionInLine(e.getDeclaration());
                            boolean wasSeparatorsOnly = false;
                            if (elementPositionInLine.isPresent()) {
                                for (int i = elementPositionInLine.get() - 1; i >= 0; i--) {
                                    if (robotLine.getLineElements().get(i) instanceof Separator) {
                                        wasSeparatorsOnly = true;
                                    } else {
                                        wasSeparatorsOnly = false;
                                        break;
                                    }
                                }

                                if (wasSeparatorsOnly) {
                                    index = elemIndex;
                                }
                            }
                        }
                        nextFound = 0;
                        nextStartFoundIndex = -1;
                    }
                }
            }
        }

        return index;
    }

    public <T extends ARobotSectionTable> boolean containsOneOfElementOffset(final AModelElement<T> e,
            final Set<Integer> startPositions) {
        boolean result = false;
        final List<RobotToken> elementTokens = e.getElementTokens();
        for (final RobotToken rt : elementTokens) {
            if (startPositions.contains(rt.getFilePosition().getOffset())) {
                result = true;
                break;
            }
        }

        return result;
    }

    public void dumpLineDirectly(final RobotFile model, final List<RobotLine> outLines, final RobotLine currentLine) {
        for (final IRobotLineElement elem : currentLine.getLineElements()) {
            getDumpLineUpdater().updateLine(model, outLines, elem);
        }

        final IRobotLineElement endOfLine = currentLine.getEndOfLine();
        if (endOfLine != null && !endOfLine.getFilePosition().isNotSet()) {
            getDumpLineUpdater().updateLine(model, outLines, endOfLine);
        }
    }

    public int getNumberOfNewToTreatAsNewUnit() {
        return NUMBER_OF_AFTER_UNIT_ELEMENTS_TO_TREAT_AS_NEW_UNIT_START;
    }

    public String getEmpty() {
        return EMPTY;
    }
}
