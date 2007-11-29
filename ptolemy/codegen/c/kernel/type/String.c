/***declareBlock***/
typedef char* StringToken;
/**/

/***funcDeclareBlock***/
Token String_new(char* s);
/**/

/***String_new***/
/* Make a new integer token from the given value. */
Token String_new(char* s) {
    Token result;
    result.type = TYPE_String;
    result.payload.String = strdup(s);
    return result;
}
/**/

/***String_delete***/
Token String_delete(Token token, ...) {   
    free(token.payload.String);    
    /* We need to return something here because all the methods are declared
     * as returning a Token so we can use them in a table of functions.
     */
    return emptyToken; 
}    
/**/

/***String_equals***/
Token String_equals(Token thisToken, ...) {
    va_list argp; 
    Token otherToken; 
    va_start(argp, thisToken);
    otherToken = va_arg(argp, Token);

    va_end(argp);
    return Boolean_new(!strcmp(thisToken.payload.String, otherToken.payload.String));
}
/**/

/***String_isCloseTo***/
/* No need to use String_isCloseTo(), we use String_equals() instead. */
}
/**/

/***String_print***/
Token String_print(Token thisToken, ...) {
    printf("\"%s\"", thisToken.payload.String);
    return emptyToken;
}
/**/

/***String_toString***/
Token String_toString(Token thisToken, ...) {
    // Guarrantee to return a new string.
    char* result = (char*) malloc(sizeof(char) * (3 + strlen(thisToken.payload.String)));
    sprintf(result, "\"%s\"", thisToken.payload.String);
    return String_new(result);
}
/**/

/***String_add***/
Token String_add(Token thisToken, ...) {
    va_list argp; 
    va_start(argp, thisToken);
    Token otherToken = va_arg(argp, Token);
	
    char* result = (char*) malloc(sizeof(char) * (1 + strlen(thisToken.payload.String) + strlen(otherToken.payload.String)));
    strcpy(result, thisToken.payload.String);
    strcat(result, otherToken.payload.String);

    va_end(argp);
    return String_new(result);
}
/**/

/***String_subtract***/
/** String_subtract is not supported. */
/**/

/***String_multiply***/
/** String_multiply is not supported. */
/**/

/***String_divide***/
/** String_divide is not supported. */
/**/

/***String_negate***/
Token String_negate(Token thisToken, ...) {
    return emptyToken;
}	
/**/

/***String_zero***/
Token String_zero(Token token, ...) {
    return String_new("");
}
/**/

/***String_one***/
/** String_one is not supported. */
/**/

/***String_clone***/
Token String_clone(Token thisToken, ...) {
    return String_new(thisToken.payload.String);
}
/**/



------------------ static functions --------------------------------------

/***String_convert***/
Token String_convert(Token token, ...) {
    char* stringPointer;
	
    switch (token.type) {
#ifdef TYPE_Boolean
    case TYPE_Boolean:
        stringPointer = BooleantoString(token.payload.Boolean);
        break;
#endif

#ifdef TYPE_Int
    case TYPE_Int:
        stringPointer = InttoString(token.payload.Int);
        break;
#endif

#ifdef TYPE_Double
    case TYPE_Double:
        stringPointer = DoubletoString(token.payload.Double);
        break;
#endif

    default:
        // FIXME: not finished
        fprintf(stderr, "String_convert(): Conversion from an unsupported type. (%d)\n", token.type);
        break;
    }
    token.payload.String = stringPointer;
    token.type = TYPE_String;
    return token;
}    
/**/

