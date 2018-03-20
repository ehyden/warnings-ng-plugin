package io.jenkins.plugins.analysis.core.model;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.junit.jupiter.api.Test;

import io.jenkins.plugins.analysis.core.JenkinsFacade;
import io.jenkins.plugins.analysis.core.model.Summary.LabelProviderFactoryFacade;
import io.jenkins.plugins.analysis.core.quality.AnalysisBuild;
import io.jenkins.plugins.analysis.core.quality.QualityGate;
import io.jenkins.plugins.analysis.core.quality.Thresholds;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import hudson.model.Result;
import hudson.model.Run;

/**
 * Tests the class {@link Summary}.
 *
 * @author Ullrich Hafner
 */
class SummaryTest {
    @Test
    void shouldProvideSummary() {
        Locale.setDefault(Locale.ENGLISH);

        LabelProviderFactoryFacade facade = mock(LabelProviderFactoryFacade.class);
        StaticAnalysisLabelProvider checkStyleLabelProvider = createLabelProvider("checkstyle", "CheckStyle");
        when(facade.get("checkstyle")).thenReturn(checkStyleLabelProvider);
        StaticAnalysisLabelProvider pmdLabelProvider = createLabelProvider("pmd", "PMD");
        when(facade.get("pmd")).thenReturn(pmdLabelProvider);

        AnalysisResult analysisRun = mock(AnalysisResult.class);
        when(analysisRun.getSizePerOrigin()).thenReturn(Maps.fixedSize.of("checkstyle", 15, "pmd", 20));
        when(analysisRun.getNewSize()).thenReturn(2);
        when(analysisRun.getFixedSize()).thenReturn(2);
        when(analysisRun.getErrorMessages()).thenReturn(Lists.immutable.empty());
        when(analysisRun.getNoIssuesSinceBuild()).thenReturn(1);

        Thresholds thresholds = new Thresholds();
        thresholds.unstableTotalAll = 1;
        when(analysisRun.getQualityGate()).thenReturn(new QualityGate(thresholds));
        when(analysisRun.getOverallResult()).thenReturn(Result.SUCCESS);
        Run<?, ?> run = mock(Run.class);
        when(run.getFullDisplayName()).thenReturn("Job #15");
        when(run.getUrl()).thenReturn("job/my-job/15");
        when(analysisRun.getReferenceBuild()).thenReturn(Optional.of(run));

        AnalysisBuild build = mock(AnalysisBuild.class);
        when(build.getNumber()).thenReturn(2);
        when(analysisRun.getBuild()).thenReturn(build);

        String actualSummary = new Summary(createLabelProvider("test", "SummaryTest"), analysisRun, facade).create();
        assertThat(actualSummary).contains("CheckStyle, PMD");
        assertThat(actualSummary).contains("No warnings for 2 builds");
        assertThat(actualSummary).contains("since build <a href=\"../1\" class=\"model-link inside\">1</a>");
        assertThat(actualSummary).containsPattern(
                createWarningsLink("<a href=\"testResult/new\">.*2 new warnings.*</a>"));
        assertThat(actualSummary).containsPattern(
                createWarningsLink("<a href=\"testResult/fixed\">.*2 fixed warnings.*</a>"));
        assertThat(actualSummary).contains("Quality gate: <img src=\"color\" class=\"icon-blue icon-lg\" alt=\"Success\" title=\"Success\"> Success");
        assertThat(actualSummary).contains("Reference build: <a href=\"absoluteUrl\">Job #15</a>");
    }

    private StaticAnalysisLabelProvider createLabelProvider(final String checkstyle, final String checkStyle) {
        JenkinsFacade jenkins = mock(JenkinsFacade.class);
        when(jenkins.getImagePath(any())).thenReturn("color");
        when(jenkins.getAbsoluteUrl(any())).thenReturn("absoluteUrl");
        return new StaticAnalysisLabelProvider(checkstyle, checkStyle, jenkins);
    }

    private Pattern createWarningsLink(final String href) {
        return Pattern.compile(href, Pattern.MULTILINE | Pattern.DOTALL);
    }
}