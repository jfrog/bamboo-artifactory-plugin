package org.jfrog.bamboo.release.scm.git;

import com.google.common.collect.Lists;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class used to execute a {@code Push} command. It has setters for all supported options and arguments of this
 * command and a {@link #call()} method to finally execute the command.
 * <p/>
 * This class is receiving its own custom {@link Transport} in order to execute the command, it is configured by the SSH
 * key-pair and pass-phrase that is sent from Bamboo.
 */
public class SshPushCommand extends PushCommand {
    private final Transport transport;
    private List<RefSpec> refSpecs;
    private ProgressMonitor monitor = NullProgressMonitor.INSTANCE;

    /**
     * @param repo
     */
    protected SshPushCommand(Repository repo, Transport transport) {
        super(repo);
        this.transport = transport;
        refSpecs = Lists.newArrayListWithCapacity(3);
    }

    /**
     * Executes the {@code push} command with all the options and parameters collected by the setter methods of this
     * class. Each instance of this class should only be used for one invocation of the command (means: one call to
     * {@link #call()})
     *
     * @return an iteration over {@link PushResult} objects
     * @throws InvalidRemoteException when called with an invalid remote uri
     * @throws JGitInternalException  a low-level exception of JGit has occurred. The original exception can be
     *                                retrieved by calling {@link Exception#getCause()}.
     */
    @Override
    public Iterable<PushResult> call() throws JGitInternalException,
            InvalidRemoteException {
        checkCallable();
        List<PushResult> pushResults = Lists.newArrayListWithCapacity(3);
        try {
            if (isForce()) {
                final List<RefSpec> orig = new ArrayList<RefSpec>(refSpecs);
                refSpecs.clear();
                for (final RefSpec spec : orig) {
                    refSpecs.add(spec.setForceUpdate(true));
                }
            }
            final RemoteConfig cfg = new RemoteConfig(repo.getConfig(), getRemote());
            transport.applyConfig(cfg);
            if (0 <= getTimeout()) {
                transport.setTimeout(getTimeout());
            }
            transport.setPushThin(isThin());
            if (getReceivePack() != null) {
                transport.setOptionReceivePack(getReceivePack());
            }
            transport.setDryRun(isDryRun());

            final Collection<RemoteRefUpdate> toPush = transport
                    .findRemoteRefUpdatesFor(getRefSpecs());
            transport.push(monitor, toPush);

            try {
                PushResult result = transport.push(monitor, toPush);
                pushResults.add(result);

            } catch (TransportException e) {
                throw new JGitInternalException(
                        JGitText.get().exceptionCaughtDuringExecutionOfPushCommand,
                        e);
            } finally {
                transport.close();
            }
        } catch (URISyntaxException e) {
            throw new InvalidRemoteException(MessageFormat.format(JGitText.get().invalidRemote, getRemote()));
        } catch (NotSupportedException e) {
            throw new JGitInternalException(JGitText.get().exceptionCaughtDuringExecutionOfPushCommand, e);
        } catch (IOException e) {
            throw new JGitInternalException(JGitText.get().exceptionCaughtDuringExecutionOfPushCommand, e);
        }
        return pushResults;
    }
}
