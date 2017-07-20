package jenkins.plugins.hipchat.model;

public class Constants {

    public static final String DEFAULT_ICON_URL = "https://bit.ly/2ctIstd";
    public static final String STATUS = "STATUS";
    public static final String HIPCHAT_MESSAGE_TEMPLATE = "HIPCHAT_MESSAGE_TEMPLATE";

    //legacy token macro names
    public static final String JOB_DISPLAY_NAME = "JOB_DISPLAY_NAME";
    public static final String PROJECT_DISPLAY_NAME = "PROJECT_DISPLAY_NAME";
    public static final String CAUSE = "CAUSE";
    public static final String URL = "URL";
    public static final String DURATION = "DURATION";
    public static final String CHANGES_OR_CAUSE = "CHANGES_OR_CAUSE";
    public static final String CHANGES = "CHANGES";
    public static final String COMMIT_MESSAGE_TEXT = "COMMIT_MESSAGE_TEXT";
    public static final String COMMIT_MESSAGE = "COMMIT_MESSAGE";
    public static final String SUCCESS_TEST_COUNT = "SUCCESS_TEST_COUNT";
    public static final String SKIPPED_TEST_COUNT = "SKIPPED_TEST_COUNT";
    public static final String FAILED_TEST_COUNT = "FAILED_TEST_COUNT";
    public static final String TEST_COUNT = "TEST_COUNT";

    //supported token macro names
    public static final String BLUE_OCEAN_URL = "BLUE_OCEAN_URL";
    public static final String BUILD_DESCRIPTION = "BUILD_DESCRIPTION";
    public static final String BUILD_DURATION = "BUILD_DURATION";
    public static final String TEST_REPORT_URL = "TEST_REPORT_URL";
    public static final String HIPCHAT_CHANGES = "HIPCHAT_CHANGES";
    public static final String HIPCHAT_CHANGES_OR_CAUSE = "HIPCHAT_CHANGES_OR_CAUSE";

    //token migration constants
    public static final String BUILD_DURATION_MACRO = "${" + BUILD_DURATION + "}";
    public static final String PROJECT_DISPLAY_NAME_MACRO = "${PROJECT_DISPLAY_NAME}";
    public static final String TOTAL_TEST_COUNT_MACRO = "${TEST_COUNTS,var=\"total\"}";
    public static final String FAILED_TEST_COUNT_MACRO = "${TEST_COUNTS,var=\"fail\"}";
    public static final String SKIPPED_TEST_COUNT_MACRO = "${TEST_COUNTS,var=\"skip\"}";
    public static final String SUCCESS_TEST_COUNT_MACRO = "${TEST_COUNTS,var=\"pass\"}";
    public static final String TEST_REPORT_URL_MACRO = "${" + TEST_REPORT_URL + "}";
    public static final String BUILD_URL_MACRO = "${BUILD_URL}";
    public static final String COMMIT_MESSAGE_MACRO = "${" + COMMIT_MESSAGE + "}";
    public static final String ESCAPED_COMMIT_MESSAGE_MACRO = "${" + COMMIT_MESSAGE + ",escape=false}";
    public static final String HIPCHAT_CHANGES_MACRO = "${" + HIPCHAT_CHANGES + "}";
    public static final String HIPCHAT_CHANGES_OR_CAUSE_MACRO = "${" + HIPCHAT_CHANGES_OR_CAUSE + "}";
}
