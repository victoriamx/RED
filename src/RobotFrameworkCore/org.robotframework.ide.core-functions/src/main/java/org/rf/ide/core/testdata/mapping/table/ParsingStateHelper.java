/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.mapping.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

@SuppressWarnings("PMD.GodClass")
public class ParsingStateHelper {

    public boolean isTypeForState(final ParsingState state, final RobotToken rt) {
        boolean result = false;

        List<RobotTokenType> typesForState = new ArrayList<>();
        if (state == ParsingState.TEST_CASE_TABLE_INSIDE || state == ParsingState.TEST_CASE_DECLARATION) {
            typesForState = RobotTokenType.getTypesForTestCasesTable();
        } else if (state == ParsingState.SETTING_TABLE_INSIDE) {
            typesForState = RobotTokenType.getTypesForSettingsTable();
        } else if (state == ParsingState.VARIABLE_TABLE_INSIDE) {
            typesForState = RobotTokenType.getTypesForVariablesTable();
        } else if (state == ParsingState.KEYWORD_TABLE_INSIDE || state == ParsingState.KEYWORD_DECLARATION) {
            typesForState = RobotTokenType.getTypesForKeywordsTable();
        }

        final List<IRobotTokenType> types = rt.getTypes();
        for (final IRobotTokenType type : types) {
            if (typesForState.contains(type)) {
                result = true;
                break;
            }
        }

        if (!result) {
            if (state == ParsingState.TEST_CASE_DECLARATION || state == ParsingState.KEYWORD_DECLARATION
                    || state == ParsingState.UNKNOWN) {
                result = (types.contains(RobotTokenType.START_HASH_COMMENT)
                        || types.contains(RobotTokenType.COMMENT_CONTINUE));

            }
        }

        return result;
    }

    public void updateStatusesForNewLine(final Stack<ParsingState> processingState) {

        boolean clean = true;
        while (clean) {
            final ParsingState currentState = getCurrentState(processingState);
            if (isTableHeader(currentState)) {
                processingState.pop();
                if (currentState == ParsingState.SETTING_TABLE_HEADER) {
                    processingState.push(ParsingState.SETTING_TABLE_INSIDE);
                } else if (currentState == ParsingState.VARIABLE_TABLE_HEADER) {
                    processingState.push(ParsingState.VARIABLE_TABLE_INSIDE);
                } else if (currentState == ParsingState.TEST_CASE_TABLE_HEADER) {
                    processingState.push(ParsingState.TEST_CASE_TABLE_INSIDE);
                } else if (currentState == ParsingState.KEYWORD_TABLE_HEADER) {
                    processingState.push(ParsingState.KEYWORD_TABLE_INSIDE);
                }

                clean = false;
            } else if (isTableInsideState(currentState)) {
                clean = false;
            } else if (isKeywordExecution(currentState)) {
                clean = false;
            } else if (isTestCaseExecution(currentState)) {
                clean = false;
            } else if (!processingState.isEmpty()) {
                processingState.pop();
            } else {
                clean = false;
            }
        }
    }

    public boolean isTestCaseExecution(final ParsingState state) {
        return state == ParsingState.TEST_CASE_DECLARATION;
    }

    public boolean isKeywordExecution(final ParsingState state) {
        return state == ParsingState.KEYWORD_DECLARATION;
    }

    public boolean isTableInsideStateInHierarchy(final ParsingState state) {
        ParsingState currentState = state;
        if (!isTableInsideState(currentState)) {
            ParsingState parentState = currentState.getPreviousState();
            while (parentState != null) {
                if (isTableInsideState(parentState)) {
                    return true;
                }
                currentState = parentState;
                parentState = currentState.getPreviousState();
            }
        } else {
            return true;
        }
        return false;
    }

    public boolean isTableInsideState(final ParsingState state) {
        return state == ParsingState.SETTING_TABLE_INSIDE || state == ParsingState.TEST_CASE_TABLE_INSIDE
                || state == ParsingState.KEYWORD_TABLE_INSIDE || state == ParsingState.VARIABLE_TABLE_INSIDE;
    }

    public boolean isTableHeader(final ParsingState state) {
        return state == ParsingState.SETTING_TABLE_HEADER || state == ParsingState.VARIABLE_TABLE_HEADER
                || state == ParsingState.TEST_CASE_TABLE_HEADER || state == ParsingState.KEYWORD_TABLE_HEADER;
    }

    public ParsingState getCurrentState(final Stack<ParsingState> processingState) {
        if (!processingState.isEmpty()) {
            return processingState.peek();
        }

        return ParsingState.UNKNOWN;
    }

    public ParsingState getState(final RobotToken rt) {
        final List<IRobotTokenType> types = rt.getTypes();
        if (types.contains(RobotTokenType.SETTINGS_TABLE_HEADER)) {
            return ParsingState.SETTING_TABLE_HEADER;
        } else if (types.contains(RobotTokenType.VARIABLES_TABLE_HEADER)) {
            return ParsingState.VARIABLE_TABLE_HEADER;
        } else if (types.contains(RobotTokenType.TEST_CASES_TABLE_HEADER)) {
            return ParsingState.TEST_CASE_TABLE_HEADER;
        } else if (types.contains(RobotTokenType.KEYWORDS_TABLE_HEADER)) {
            return ParsingState.KEYWORD_TABLE_HEADER;
        }

        return ParsingState.UNKNOWN;
    }

    public ParsingState getFirstTableHeaderState(final Stack<ParsingState> processingState) {
        return processingState.stream().filter(this::isTableHeader).findFirst().orElse(ParsingState.UNKNOWN);
    }

    public ParsingState getLastNotCommentState(final Stack<ParsingState> processingState) {
        return processingState.stream()
                .filter(s -> s != ParsingState.COMMENT)
                .reduce((first, second) -> second)
                .orElse(ParsingState.UNKNOWN);
    }
}
