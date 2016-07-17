package com.akoscz.googleanalytics.util;

import lombok.NonNull;

/**
 * This class parses a Throwable and produces a condensed description string of the following format:
 * 		exception_class_name("exception_message"); @class_name:method_name:line_number; {thread_name}
 *
 *  where
 *  	[exception_class_name] is the class name of the Throwable that caused this exception.
 *  	[exception_message] is the message string the Throwable was created with.
 *  	[class_name] is the class name of the Object in which the exception occurred.
 *  	[method_name] is the name of the method in which the exception occurred.
 *  	[line_number] is the line number on which the exception occurred.
 *  	[thread_name] is the name of the thread on which the exception occurred.
 *
 *  For example:
 * 		IllegalArgumentException("'clientId' cannot be null!"); @GoogleAnalytics:validateRequiredParams:329; {main}
 */
public class ExceptionParser {
	private final String[] includePackages;

	public ExceptionParser(String... packages) {
		includePackages = packages;
	}

	protected StackTraceElement getBestStackTraceElement(@NonNull Throwable throwable) {
		// get all the elements in the stack trace
		StackTraceElement[] elements = throwable.getStackTrace();
		if (elements == null || elements.length == 0)
			return null;

		// look through all the elements in the stack trace
		for (StackTraceElement element : elements) {
			String className = element.getClassName();
			// find the first element which package name in the list of packages we are interested in
			for (String packageString : includePackages) {
				if (className.startsWith(packageString)) {
					return element;
				}
			}
		}
		// we didn't match any of the packages we are interested in.  Default to the first element in the stacktrace
		return elements[0];
	}

	protected Throwable getCause(@NonNull Throwable throwable) {
		Throwable result = throwable;
		// find the very last cause in this Throwable
		while (result.getCause() != null) {
			result = result.getCause();
		}
		return result;
	}

	/**
	 * Produces an exception description string for the given Throwable parameter.
	 * @param threadName Name of the thread on which the exception occurred
	 * @param throwable The throwable for which we want to generate a description string for.
     * @return The description string representing the Throwable parameter
     */
	public String getDescription(String threadName, @NonNull Throwable throwable) {
		Throwable cause = getCause(throwable);

		StringBuilder descriptionBuilder = new StringBuilder();
		descriptionBuilder
				.append(cause.getClass().getSimpleName())
				.append("(\"")
				.append(cause.getMessage())
				.append("\")");

		StackTraceElement element = getBestStackTraceElement(cause);
		if (element != null) {
			String[] classNameParts = element.getClassName().split("\\.");

			String className = (classNameParts == null || classNameParts.length <= 0)
					? "unknown" : classNameParts[classNameParts.length - 1];

			descriptionBuilder.append(String.format("; @%s:%s:%s;",
							className,
							element.getMethodName(),
							Integer.valueOf(element.getLineNumber())));

		}
		if (threadName != null) {
			descriptionBuilder.append(String.format(" {%s}", threadName));
		}
		return descriptionBuilder.toString();
	}
}