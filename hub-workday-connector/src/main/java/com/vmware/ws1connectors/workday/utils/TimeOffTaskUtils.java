/*
 * Project Workday Connector
 * (c) 2019-2020 VMware, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.vmware.ws1connectors.workday.utils;

import com.vmware.ws1connectors.workday.models.Descriptor;
import com.vmware.ws1connectors.workday.models.InboxTask;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffEntry;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffEvent;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffItem;
import com.vmware.ws1connectors.workday.models.timeoff.TimeOffTask;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.vmware.ws1connectors.workday.utils.ArgumentUtils.checkArgumentNotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class TimeOffTaskUtils {
    private static final char HYPHEN = '-';
    private static final String LOCALE = "Locale";
    private static final String INBOX_TASK = "InboxTask";
    private static final String TIME_OFF_EVENT = "TimeOffEvent";

    public static TimeOffTask createTimeOffTask(final InboxTask inboxTask, final TimeOffEvent timeOffEvent, final Locale locale) {
        checkArgumentNotNull(locale, LOCALE);
        checkArgumentNotNull(inboxTask, INBOX_TASK);
        checkArgumentNotNull(timeOffEvent, TIME_OFF_EVENT);

        final Descriptor timeOffEventDescriptor = timeOffEvent.getTimeOffEventDescriptor();
        LOGGER.info("Merging time off event view and business process summary views together into TimeOffTask. Timeoff request id: {}, intent: {}, link: {}.",
            timeOffEventDescriptor.getId(), inboxTask.getStepType().getDescriptor(), timeOffEventDescriptor.getHref());
        final List<TimeOffItem> timeOffItems = getTimeOffApprovalTaskItems(timeOffEvent, locale);

        final String subject = timeOffEvent.getSubject().getDescriptor();
        final LocalDate firstDayOfTimeOff = getFirstDayOfTimeOff(timeOffItems);
        final TimeOffTask.Builder timeOffTaskBuilder = TimeOffTask.builder()
            .timeOffTaskDescriptor(timeOffEventDescriptor)
            .subject(subject)
            .assignedOn(inboxTask.getAssigned())
            .overallStatus(timeOffEvent.getTransactionStatus().getDescriptor())
            .startDate(firstDayOfTimeOff)
            .endDate(getLastDayOfTimeOff(timeOffItems))
            .totalTimeOffDuration(computeTotalTimeOffDuration(timeOffEvent, timeOffItems))
            .inboxTaskId(inboxTask.getId())
            .inboxTaskHref(inboxTask.getHref())
            .timeOffItems(timeOffItems);
        return timeOffTaskBuilder.build();
    }

    private static LocalDate getFirstDayOfTimeOff(final List<TimeOffItem> timeOffItems) {
        return timeOffItems.get(0).getDate();
    }

    private static LocalDate getLastDayOfTimeOff(final List<TimeOffItem> timeOffItems) {
        return timeOffItems.get(timeOffItems.size() - 1).getDate();
    }

    private static List<TimeOffItem> getTimeOffApprovalTaskItems(final TimeOffEvent timeOffEvent, final Locale locale) {
        return timeOffEvent.getTimeOffEntries().stream()
            .map(timeOffEntry -> convertToTimeOffApprovalItem(timeOffEntry, locale))
            .sorted(Comparator.comparing(TimeOffItem::getDate))
            .collect(Collectors.toList());
    }

    private static TimeOffItem convertToTimeOffApprovalItem(final TimeOffEntry timeOffEntry, final Locale locale) {
        final TimeOffItem.Builder timeOffItemBuilder = TimeOffItem.builder()
                .date(timeOffEntry.getDate())
                .dayOfWeek(getDayOfWeek(timeOffEntry, locale))
                .requestedTimeOffQuantity(timeOffEntry.getUnits());
        Optional.ofNullable(timeOffEntry.getTimeOff())
                .ifPresent(timeOff -> timeOffItemBuilder.type(timeOff.getDescriptor()));
        Optional.ofNullable(timeOffEntry.getUnitOfTime())
                .ifPresent(unit -> timeOffItemBuilder.unitOfTime(unit.getDescriptor()));
        return timeOffItemBuilder.build();
    }

    private static String getDayOfWeek(final TimeOffEntry timeOffEntry, final Locale locale) {
        return timeOffEntry.getDate()
            .atStartOfDay()
            .getDayOfWeek()
            .getDisplayName(TextStyle.FULL, locale);
    }

    private static String computeTotalTimeOffDuration(final TimeOffEvent timeOffEvent, final List<TimeOffItem> timeOffItems) {
        final int totalTimeOffDuration = timeOffItems.stream()
            .map(TimeOffItem::getRequestedTimeOffQuantity)
            .mapToInt(NumberUtils::toInt)
            .sum();
        final TimeOffEntry timeOffEntry = timeOffEvent.getTimeOffEntries().get(0);
        final StringBuilder totalTimeOffDurationText = new StringBuilder(String.valueOf(totalTimeOffDuration))
                .append(StringUtils.SPACE)
                .append(timeOffEntry.getUnitOfTime().getDescriptor());
        Optional.ofNullable(timeOffEntry.getTimeOff())
            .ifPresent(timeOff -> totalTimeOffDurationText.append(HYPHEN).append(timeOff.getDescriptor()));
        return totalTimeOffDurationText.toString();
    }

}
