package org.grobid.core.exceptions;

public enum GrobidExceptionStatus {
    BAD_INPUT_DATA,
    TAGGING_ERROR,
    PARSING_ERROR,
    TIMEOUT,
    TOO_MANY_BLOCKS,
    NO_BLOCKS,
    PDFALTO_CONVERSION_FAILURE,
    TOO_MANY_TOKENS,
    MEMORY_LIMIT_EXCEEDED,
    GENERAL
}
