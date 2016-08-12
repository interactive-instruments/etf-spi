/**
 * Copyright 2010-2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.interactive_instruments.etf.testdriver.adapter;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

/**
 * The TestResultListener is used to report failures and messages
 * during a test run as well as adding attachments and logging messages
 * (info, debug, error) that are written to a log and linked to the
 * test task result.
 *
 * The TestResultListener is exposed by a test driver, is somehow injected into a
 * test engine and consumed during a test run by a test driver adapter.
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public interface TestResultListener {

	/**
	 * Called when a test item is run
	 *
	 * @param eid Test Model Item ID
	 * @return eid of the recorded test result item
	 *
	 * @throws IllegalArgumentException if the eid is invalid or the test can't be started in the current context
	 * @throws IllegalStateException if test already has been started or ended
	 */
	String start(final String eid) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Called just after a test item has been run
	 *
	 * @param eid Test Model Item ID
	 * @return eid of the recorded test result item
	 *
	 * @throws IllegalArgumentException if test already has been ended
	 * @throws IllegalStateException if test already has been ended or hasn't been started yet
	 */
	String end(final String eid, final String status) throws IllegalArgumentException, IllegalStateException;

	/**
	 * Add a message
	 *
	 * @param translationTemplateId Translation Template ID
	 */
	void addMessage(final String translationTemplateId);

	/**
	 * Add a message with translation parameters as token value pairs
	 *
	 * @param translationTemplateId Translation Template ID
	 * @param tokenValuePairs Translation Template message as token value pair
	 */
	void addMessage(final String translationTemplateId, final Map<String, String> tokenValuePairs);

	/**
	 * Add a message with translation parameters as token value pairs
	 *
	 * @param translationTemplateId Translation Template ID
	 * @param tokensAndValues Translation Template message as alternating tokens and values
	 * @throws IllegalArgumentException if number of tokensAndValues arguments is odd
	 */
	void addMessage(final String translationTemplateId, final String... tokensAndValues);

	/**
	 * An output stream accepts output bytes and sends them to some sink.
	 */
	abstract class AttachmentOutputStream extends OutputStream {
		/**
		 * Returns the ID of the attachment
		 *
		 * @return eid of the created Attachment
		 */
		public abstract String getId();

		/**
		 * Returns the label of the Attachment
		 *
		 * @return label of the Attachment
		 */
		public abstract String getLabel();
	}

	/**
	 * Create an attachment
	 *
	 * @param label Label for the attachment
	 * @param outputStream outputStream
	 * @param encoding encoding of the data
	 * @param mimeType mime type or null if the type should be auto detected
	 *
	 * @return {@link AttachmentOutputStream}
	 */
	AttachmentOutputStream createAttachment(
			final String label,
			final OutputStream outputStream,
			final String encoding,
			final String mimeType);

	/**
	 * Returns a directory which can be used to store data temporary
	 * during the test run.
	 * The directory and its content will be deleted after the test run!
	 *
	 * @return temporary directory file
	 *
	 */
	File getTempDir();

	/**
	 * Report an internal error and abort the test
	 *
	 * @param translationTemplateId Translation Template ID
	 * @param tokenValuePairs Translation Template message as Token Value pair
	 * @param e Exception
	 */
	void internalError(
			final String translationTemplateId,
			final Map<String, String> tokenValuePairs,
			final Throwable e);

	/**
	 * Report an error and abort the test
	 *
	 * @param e Exception
	 */
	void internalError(final Throwable e);

	/**
	 * Info message which is written to the log file
	 *
	 * @param message message as String
	 */
	void log(final String message);

	/**
	 * Error message which is written to the log file
	 *
	 * @param message message as String
	 */
	void error(final String message);

	/**
	 * Debug message which is written to the log file
	 *
	 * @param message message as String
	 */
	void debug(final String message);
}
