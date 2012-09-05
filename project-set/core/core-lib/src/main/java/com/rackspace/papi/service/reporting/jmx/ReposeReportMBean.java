package com.rackspace.papi.service.reporting.jmx;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import java.util.List;

public interface ReposeReportMBean {
    static final String OBJECT_NAME = "com.rackspace.papi.service.reporting:type=ReposeReport";
    String getTotal400sReposeToClient();
    String getTotal500sReposeToClient();

    List<CompositeData> getDestinationInfo() throws OpenDataException;
}
