/*
 * Copyright (C) 2010, Chris Aniszczyk <caniszczyk@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jfrog.bamboo.release.vcs.git;

import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.InvalidTagNameException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

/**
 * A class used to execute a {@code Tag} command. It has setters for all supported options and arguments of this command
 * and a {@link #call()} method to finally execute the command.
 *
 * @see <a href="http://www.kernel.org/pub/software/scm/git/docs/git-tag.html" >Git documentation about Tag</a>
 */
public class DeleteTagCommand extends GitCommand<Object> {
    private RevObject id;

    private String name;

    private String message;

    private PersonIdent tagger;

    private boolean signed;

    private boolean forceUpdate;

    /**
     * @param repo
     */
    protected DeleteTagCommand(Repository repo) {
        super(repo);
    }

    /**
     * Executes the {@code tag} command with all the options and parameters collected by the setter methods of this
     * class. Each instance of this class should only be used for one invocation of the command (means: one call to
     * {@link #call()})
     *
     * @return a {@link RevTag} object representing the successful tag
     * @throws NoHeadException       when called on a git repo without a HEAD reference
     * @throws JGitInternalException a low-level exception of JGit has occurred. The original exception can be retrieved
     *                               by calling {@link Exception#getCause()}. Expect only {@code IOException's} to be
     *                               wrapped.
     */
    @Override
    public RefUpdate.Result call() throws JGitInternalException,
            ConcurrentRefUpdateException, InvalidTagNameException, NoHeadException {
        checkCallable();

        RepositoryState state = repo.getRepositoryState();
        processOptions(state);
        Map<String, Ref> tags = repo.getTags();
        for (Map.Entry<String, Ref> entry : tags.entrySet()) {
            if (entry.getKey().equals(getName())) {
                Ref value = entry.getValue();
                RefUpdate update;
                try {
                    update = repo.updateRef(value.getName());
                    update.setForceUpdate(true);
                    return update.delete();
                } catch (IOException e) {
                    throw new JGitInternalException(
                            JGitText.get().exceptionCaughtDuringExecutionOfTagCommand, e);
                }
            }
        }
        throw new JGitInternalException(
                JGitText.get().exceptionCaughtDuringExecutionOfTagCommand);
    }

    /**
     * Sets default values for not explicitly specified options. Then validates that all required data has been
     * provided.
     *
     * @param state the state of the repository we are working on
     * @throws InvalidTagNameException       if the tag name is null or invalid
     * @throws UnsupportedOperationException if the tag is signed (not supported yet)
     */
    private void processOptions(RepositoryState state)
            throws InvalidTagNameException {
        if (tagger == null) {
            tagger = new PersonIdent(repo);
        }
        if (name == null || !Repository.isValidRefName(Constants.R_TAGS + name)) {
            throw new InvalidTagNameException(MessageFormat.format(JGitText
                    .get().tagNameInvalid, name == null ? "<null>" : name));
        }
        if (signed) {
            throw new UnsupportedOperationException(
                    JGitText.get().signingNotSupportedOnTag);
        }
    }

    /**
     * @return the tag name used for the <code>tag</code>
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the tag name used for the {@code tag}
     * @return {@code this}
     */
    public DeleteTagCommand setName(String name) {
        checkCallable();
        this.name = name;
        return this;
    }

    /**
     * @return the tag message used for the <code>tag</code>
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the tag message used for the {@code tag}
     * @return {@code this}
     */
    public DeleteTagCommand setMessage(String message) {
        checkCallable();
        this.message = message;
        return this;
    }

    /**
     * @return whether the tag is signed
     */
    public boolean isSigned() {
        return signed;
    }

    /**
     * If set to true the Tag command creates a signed tag object. This corresponds to the parameter -s on the command
     * line.
     *
     * @param signed
     * @return {@code this}
     */
    public DeleteTagCommand setSigned(boolean signed) {
        this.signed = signed;
        return this;
    }

    /**
     * @return the tagger of the tag
     */
    public PersonIdent getTagger() {
        return tagger;
    }

    /**
     * Sets the tagger of the tag. If the tagger is null, a PersonIdent will be created from the info in the
     * repository.
     *
     * @param tagger
     * @return {@code this}
     */
    public DeleteTagCommand setTagger(PersonIdent tagger) {
        this.tagger = tagger;
        return this;
    }

    /**
     * @return the object id of the tag
     */
    public RevObject getObjectId() {
        return id;
    }

    /**
     * Sets the object id of the tag. If the object id is null, the commit pointed to from HEAD will be used.
     *
     * @param id
     * @return {@code this}
     */
    public DeleteTagCommand setObjectId(RevObject id) {
        this.id = id;
        return this;
    }

    /**
     * @return is this a force update
     */
    public boolean isForceUpdate() {
        return forceUpdate;
    }

    /**
     * If set to true the Tag command may replace an existing tag object. This corresponds to the parameter -f on the
     * command line.
     *
     * @param forceUpdate
     * @return {@code this}
     */
    public DeleteTagCommand setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
        return this;
    }

}
