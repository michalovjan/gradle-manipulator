package org.jboss.gm.analyzer.alignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.commonjava.maven.ext.core.state.DependencyState;
import org.commonjava.maven.ext.io.rest.RestException;
import org.commonjava.maven.ext.io.rest.Translator;
import org.jboss.gm.common.Configuration;
import org.jboss.gm.common.logging.GMLogger;
import org.jboss.gm.common.utils.RESTUtils;
import org.slf4j.Logger;

import static org.commonjava.maven.ext.core.state.DependencyState.DependencyPrecedence.NONE;

/**
 * An implementation of {@link AlignmentService} that uses the Dependency Analyzer service
 * in order to get the proper aligned versions of dependencies (as well as the version of the project itself)
 *
 * The heavy lifting is done by {@link org.commonjava.maven.ext.io.rest.DefaultTranslator}
 */
public class DAAlignmentService implements AlignmentService {

    private final Logger logger = GMLogger.getLogger(getClass());

    private final Translator restEndpoint;

    private final DependencyState.DependencyPrecedence dependencySource;

    public DAAlignmentService(Configuration configuration) {
        dependencySource = configuration.dependencyConfiguration();

        final String endpointUrl = configuration.daEndpoint();

        if (endpointUrl == null && (dependencySource != NONE)) {
            throw new ManipulationUncheckedException("'{}' must be configured in order for dependency scanning to work",
                    Configuration.DA);
        }

        restEndpoint = RESTUtils.getTranslator(configuration);
    }

    @Override
    public Response align(AlignmentService.Request request) throws RestException {
        final List<ProjectVersionRef> translateRequest = new ArrayList<>(request.getDependencies().size() + 1);

        if (dependencySource == NONE) {
            logger.warn("No dependencySource configured ; unable to call endpoint");
            return new Response(Collections.emptyMap());
        }

        translateRequest.addAll(request.getProject());
        translateRequest.addAll(request.getDependencies());

        logger.debug("Passing {} GAVs following into the REST client api {} ", translateRequest.size(), translateRequest);
        logger.info("Calling REST client with {} GAVS...", translateRequest.size());
        final Map<ProjectVersionRef, String> translationMap = restEndpoint.translateVersions(translateRequest);
        logger.info("REST Client returned {} ", translationMap);

        Response result = new Response(translationMap);

        if (!request.getProject().isEmpty()) {
            logger.info("Retrieving project version {} and returning {} ", request.getProject().get(0),
                    translationMap.get(request.getProject().get(0)));
            result.setNewProjectVersion(translationMap.get(request.getProject().get(0)));
        }
        return result;
    }
}
