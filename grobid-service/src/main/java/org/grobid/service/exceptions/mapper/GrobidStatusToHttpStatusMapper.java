package org.grobid.service.exceptions.mapper;

import org.grobid.core.exceptions.GrobidExceptionStatus;

import jakarta.ws.rs.core.Response;

public class GrobidStatusToHttpStatusMapper {
    public static Response.Status getStatusCode(GrobidExceptionStatus status) {
        switch (status) {
            case BAD_INPUT_DATA :
                return Response.Status.BAD_REQUEST;
            case TAGGING_ERROR :
                return Response.Status.INTERNAL_SERVER_ERROR;
            case PARSING_ERROR :
                return Response.Status.INTERNAL_SERVER_ERROR;
            case TIMEOUT :
                return Response.Status.CONFLICT;
            case TOO_MANY_BLOCKS :
                return Response.Status.CONFLICT;
            case NO_BLOCKS :
                return Response.Status.BAD_REQUEST;
            case PDFALTO_CONVERSION_FAILURE :
                return Response.Status.INTERNAL_SERVER_ERROR;
            case TOO_MANY_TOKENS :
                return Response.Status.CONFLICT;
            case GENERAL :
                return Response.Status.INTERNAL_SERVER_ERROR;
            default :
                return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }
}
