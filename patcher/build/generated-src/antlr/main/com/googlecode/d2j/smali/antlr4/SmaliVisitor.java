// Generated from com\googlecode\d2j\smali\antlr4\Smali.g4 by ANTLR 4.5
package com.googlecode.d2j.smali.antlr4;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SmaliParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SmaliVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sFiles}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSFiles(SmaliParser.SFilesContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sFile}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSFile(SmaliParser.SFileContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sSource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSSource(SmaliParser.SSourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sSuper}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSSuper(SmaliParser.SSuperContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sInterface}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSInterface(SmaliParser.SInterfaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sMethod}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSMethod(SmaliParser.SMethodContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sField}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSField(SmaliParser.SFieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sAccList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSAccList(SmaliParser.SAccListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sAnnotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSAnnotation(SmaliParser.SAnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sSubannotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSSubannotation(SmaliParser.SSubannotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSParameter(SmaliParser.SParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sAnnotationKeyName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSAnnotationKeyName(SmaliParser.SAnnotationKeyNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sAnnotationValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSAnnotationValue(SmaliParser.SAnnotationValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sBaseValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSBaseValue(SmaliParser.SBaseValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sArrayValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSArrayValue(SmaliParser.SArrayValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSInstruction(SmaliParser.SInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fline}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFline(SmaliParser.FlineContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#flocal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlocal(SmaliParser.FlocalContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fend}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFend(SmaliParser.FendContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#frestart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrestart(SmaliParser.FrestartContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fprologue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFprologue(SmaliParser.FprologueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fepiogue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFepiogue(SmaliParser.FepiogueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fregisters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFregisters(SmaliParser.FregistersContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#flocals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlocals(SmaliParser.FlocalsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fcache}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFcache(SmaliParser.FcacheContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fcacheall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFcacheall(SmaliParser.FcacheallContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#sLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSLabel(SmaliParser.SLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fpackageswitch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFpackageswitch(SmaliParser.FpackageswitchContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fspareswitch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFspareswitch(SmaliParser.FspareswitchContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#farraydata}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFarraydata(SmaliParser.FarraydataContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#f0x}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitF0x(SmaliParser.F0xContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#f0t}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitF0t(SmaliParser.F0tContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#f1x}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitF1x(SmaliParser.F1xContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fconst}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFconst(SmaliParser.FconstContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#ff1c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFf1c(SmaliParser.Ff1cContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#ft2c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFt2c(SmaliParser.Ft2cContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#ff2c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFf2c(SmaliParser.Ff2cContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#f2x}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitF2x(SmaliParser.F2xContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#f3x}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitF3x(SmaliParser.F3xContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#ft5c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFt5c(SmaliParser.Ft5cContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fm5c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFm5c(SmaliParser.Fm5cContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fmrc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFmrc(SmaliParser.FmrcContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fm45cc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFm45cc(SmaliParser.Fm45ccContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fm4rcc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFm4rcc(SmaliParser.Fm4rccContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fmcustomc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFmcustomc(SmaliParser.FmcustomcContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#fmcustomrc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFmcustomrc(SmaliParser.FmcustomrcContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#ftrc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFtrc(SmaliParser.FtrcContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#f31t}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitF31t(SmaliParser.F31tContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#f1t}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitF1t(SmaliParser.F1tContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#f2t}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitF2t(SmaliParser.F2tContext ctx);
	/**
	 * Visit a parse tree produced by {@link SmaliParser#f2sb}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitF2sb(SmaliParser.F2sbContext ctx);
}