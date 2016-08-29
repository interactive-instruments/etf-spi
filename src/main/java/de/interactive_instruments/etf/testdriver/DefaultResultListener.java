/*
 * Copyright ${year} interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.interactive_instruments.etf.testdriver;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dto.result.TestResultStatus;
import org.slf4j.Logger;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class DefaultResultListener implements TestResultListener {

	private final TestRunLogger logger;
	private final IFile tmpDir;
	private final IFile attachmentDir;
	private final IFile dsDir;

	public DefaultResultListener(final TestRunLogger logger, final IFile tmpDir, final IFile attachmentDir) {
		this.logger = logger;
		this.tmpDir = tmpDir;
		this.attachmentDir = attachmentDir;
		// todo remove
		this.dsDir = null;
	}

	/**
	 * will be removed in version 2.1.0
	 */
	@Deprecated
	public DefaultResultListener(final TestRunLogger logger, final IFile tmpDir, final IFile attachmentDir, final IFile dsDir) {
		this.logger = logger;
		this.tmpDir = tmpDir;
		this.attachmentDir = attachmentDir;
		this.dsDir = dsDir;
	}

	@Override public File getTempDir() {
		return tmpDir;
	}

	@Override public IFile getAttachmentDir() {
		return attachmentDir;
	}

	@Override public IFile getDsDir() {
		return dsDir;
	}

	@Override public TestRunLogger getLogger() {
		return this.logger;
	}





	@Override public String start(final String testModelItemId) throws IllegalArgumentException, IllegalStateException {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public String end(final String testModelItemId, final String status) throws IllegalArgumentException, IllegalStateException {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public TestResultStatus status(final String testModelItemId) throws IllegalArgumentException {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public boolean statusEqualsAny(final String testModelItemId, final String... testResultStatus) throws IllegalArgumentException {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public void addMessage(final String translationTemplateId) {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public void addMessage(final String translationTemplateId, final Map<String, String> tokenValuePairs) {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public void addMessage(final String translationTemplateId, final String... tokensAndValues) {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public AttachmentOutputStream createAttachment(final String label, final OutputStream outputStream, final String encoding, final String mimeType) {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public void internalError(final String translationTemplateId, final Map<String, String> tokenValuePairs, final Throwable e) {
		throw new IllegalStateException("Unimplemented");
	}

	@Override public void internalError(final Throwable e) {
		throw new IllegalStateException("Unimplemented");
	}

}
