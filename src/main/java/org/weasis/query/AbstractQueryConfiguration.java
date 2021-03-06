package org.weasis.query;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.util.LangUtil;
import org.weasis.core.api.util.StringUtil;
import org.weasis.dicom.mf.ArcQuery.ViewerMessage;
import org.weasis.dicom.mf.Patient;
import org.weasis.dicom.mf.QueryResult;
import org.weasis.dicom.mf.Series;
import org.weasis.dicom.mf.Study;
import org.weasis.dicom.mf.WadoParameters;
import org.weasis.servlet.ConnectorProperties;

public abstract class AbstractQueryConfiguration implements QueryResult {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractQueryConfiguration.class);

    protected final List<Patient> patients;
    protected ViewerMessage viewerMessage;
    protected final Properties properties;

    public AbstractQueryConfiguration(Properties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("properties cannot be null!");
        }
        this.properties = properties;
        this.patients = new ArrayList<>();
    }

    public abstract void buildFromPatientID(CommonQueryParams params, String... patientIDs);

    public abstract void buildFromStudyInstanceUID(CommonQueryParams params, String... studyInstanceUIDs);

    public abstract void buildFromStudyAccessionNumber(CommonQueryParams params, String... accessionNumbers);

    public abstract void buildFromSeriesInstanceUID(CommonQueryParams params, String... seriesInstanceUIDs);

    public abstract void buildFromSopInstanceUID(CommonQueryParams params, String... sopInstanceUIDs);

    @Override
    public WadoParameters getWadoParameters() {
        String wadoQueriesURL =
            properties.getProperty("arc.wado.url", properties.getProperty("server.base.url") + "/wado");
        boolean onlysopuid = LangUtil.getEmptytoFalse(properties.getProperty("wado.onlysopuid"));
        String addparams = properties.getProperty("wado.addparams", "");
        String overrideTags = properties.getProperty("wado.override.tags");
        // If the web server requires an authentication (arc.web.login=user:pwd)
        String webLogin = properties.getProperty("arc.web.login");
        if (StringUtil.hasText(webLogin)) {
            webLogin = Base64.getEncoder().encodeToString(webLogin.trim().getBytes()); 
        }
        String httpTags = properties.getProperty("wado.httpTags");

        WadoParameters wado =
            new WadoParameters(getArchiveID(), wadoQueriesURL, onlysopuid, addparams, overrideTags, webLogin);
        if (StringUtil.hasText(httpTags)) {
            for (String tag : httpTags.split(",")) {
                String[] val = tag.split(":");
                if (val.length == 2) {
                    wado.addHttpTag(val[0].trim(), val[1].trim());
                }
            }
        }
        return wado;
    }

    @Override
    public void removePatientId(List<String> patientIdList) {
        if (patientIdList != null && !patientIdList.isEmpty()) {
            for (int i = patients.size() - 1; i >= 0; i--) {
                if (!patientIdList.contains(patients.get(i).getPatientID())) {
                    patients.remove(i);
                }
            }
        }
    }

    @Override
    public void removeStudyUid(List<String> studyUidList) {
        if (studyUidList != null && !studyUidList.isEmpty()) {
            for (Patient p : patients) {
                List<Study> studies = p.getStudies();
                for (int i = studies.size() - 1; i >= 0; i--) {
                    if (!studyUidList.contains(studies.get(i).getStudyInstanceUID())) {
                        studies.remove(i);
                    }
                }
            }
        }
    }

    @Override
    public void removeAccessionNumber(List<String> accessionNumberList) {
        if (accessionNumberList != null && !accessionNumberList.isEmpty()) {
            for (Patient p : patients) {
                List<Study> studies = p.getStudies();
                for (int i = studies.size() - 1; i >= 0; i--) {
                    if (!accessionNumberList.contains(studies.get(i).getAccessionNumber())) {
                        studies.remove(i);
                    }
                }
            }
        }
    }

    @Override
    public void removeSeriesUid(List<String> seriesUidList) {
        if (seriesUidList != null && !seriesUidList.isEmpty()) {
            for (Patient p : patients) {
                List<Study> studies = p.getStudies();
                for (int i = studies.size() - 1; i >= 0; i--) {
                    List<Series> series = studies.get(i).getSeriesList();
                    for (int k = series.size() - 1; k >= 0; k--) {
                        if (!seriesUidList.contains(series.get(k).getSeriesInstanceUID())) {
                            series.remove(k);
                            if (series.isEmpty()) {
                                studies.remove(i);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<Patient> getPatients() {
        return patients;
    }

    @Override
    public ViewerMessage getViewerMessage() {
        return viewerMessage;
    }

    @Override
    public void setViewerMessage(ViewerMessage viewerMessage) {
        this.viewerMessage = viewerMessage;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getArchiveID() {
        return properties.getProperty("arc.id");
    }

    public String getArchiveConfigName() {
        return properties.getProperty(ConnectorProperties.CONFIG_FILENAME);
    }

}