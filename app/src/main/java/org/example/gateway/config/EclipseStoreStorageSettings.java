/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.example.gateway.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Subset of official EclipseStore storage properties consumed by the gateway.
 * These settings are loaded from per-store YAML files.
 */
public class EclipseStoreStorageSettings {

    @JsonProperty("storage-directory")
    @JsonAlias("storage-path")
    private String storageDirectory;

    @JsonProperty("deletion-directory")
    private String deletionDirectory;

    @JsonProperty("truncation-directory")
    private String truncationDirectory;

    @JsonProperty("backup-directory")
    private String backupDirectory;

    @JsonProperty("channel-count")
    private Integer channelCount;

    @JsonProperty("channel-directory-prefix")
    private String channelDirectoryPrefix;

    @JsonProperty("data-file-prefix")
    private String dataFilePrefix;

    @JsonProperty("data-file-suffix")
    private String dataFileSuffix;

    @JsonProperty("transaction-file-prefix")
    private String transactionFilePrefix;

    @JsonProperty("transaction-file-suffix")
    private String transactionFileSuffix;

    @JsonProperty("type-dictionary-file-name")
    private String typeDictionaryFileName;

    @JsonProperty("rescued-file-suffix")
    private String rescuedFileSuffix;

    @JsonProperty("lock-file-name")
    private String lockFileName;

    @JsonProperty("housekeeping-interval")
    private Long housekeepingInterval;

    @JsonProperty("housekeeping-time-budget")
    private Long housekeepingTimeBudget;

    @JsonProperty("housekeeping-maximum-time-budget")
    private Long housekeepingMaximumTimeBudget;

    @JsonProperty("housekeeping-adaptive")
    private Boolean housekeepingAdaptive;

    @JsonProperty("housekeeping-increase-threshold")
    private Long housekeepingIncreaseThreshold;

    @JsonProperty("housekeeping-increase-amount")
    private Long housekeepingIncreaseAmount;

    @JsonProperty("entity-cache-threshold")
    private Long entityCacheThreshold;

    @JsonProperty("entity-cache-timeout")
    private Long entityCacheTimeout;

    @JsonProperty("data-file-minimum-size")
    private Integer dataFileMinimumSize;

    @JsonProperty("data-file-maximum-size")
    private Integer dataFileMaximumSize;

    @JsonProperty("data-file-minimum-use-ratio")
    private Double dataFileMinimumUseRatio;

    @JsonProperty("data-file-cleanup-head-file")
    private Boolean dataFileCleanupHeadFile;

    @JsonProperty("transaction-file-maximum-size")
    private Integer transactionFileMaximumSize;

    @JsonProperty("azure-connection-string")
    private String azureConnectionString;

    @JsonProperty("azure-container")
    private String azureContainer;

    public String getStorageDirectory() { return storageDirectory; }
    public void setStorageDirectory(String storageDirectory) { this.storageDirectory = storageDirectory; }

    public String getDeletionDirectory() { return deletionDirectory; }
    public void setDeletionDirectory(String deletionDirectory) { this.deletionDirectory = deletionDirectory; }

    public String getTruncationDirectory() { return truncationDirectory; }
    public void setTruncationDirectory(String truncationDirectory) { this.truncationDirectory = truncationDirectory; }

    public String getBackupDirectory() { return backupDirectory; }
    public void setBackupDirectory(String backupDirectory) { this.backupDirectory = backupDirectory; }

    public Integer getChannelCount() { return channelCount; }
    public void setChannelCount(Integer channelCount) { this.channelCount = channelCount; }

    public String getChannelDirectoryPrefix() { return channelDirectoryPrefix; }
    public void setChannelDirectoryPrefix(String channelDirectoryPrefix) { this.channelDirectoryPrefix = channelDirectoryPrefix; }

    public String getDataFilePrefix() { return dataFilePrefix; }
    public void setDataFilePrefix(String dataFilePrefix) { this.dataFilePrefix = dataFilePrefix; }

    public String getDataFileSuffix() { return dataFileSuffix; }
    public void setDataFileSuffix(String dataFileSuffix) { this.dataFileSuffix = dataFileSuffix; }

    public String getTransactionFilePrefix() { return transactionFilePrefix; }
    public void setTransactionFilePrefix(String transactionFilePrefix) { this.transactionFilePrefix = transactionFilePrefix; }

    public String getTransactionFileSuffix() { return transactionFileSuffix; }
    public void setTransactionFileSuffix(String transactionFileSuffix) { this.transactionFileSuffix = transactionFileSuffix; }

    public String getTypeDictionaryFileName() { return typeDictionaryFileName; }
    public void setTypeDictionaryFileName(String typeDictionaryFileName) { this.typeDictionaryFileName = typeDictionaryFileName; }

    public String getRescuedFileSuffix() { return rescuedFileSuffix; }
    public void setRescuedFileSuffix(String rescuedFileSuffix) { this.rescuedFileSuffix = rescuedFileSuffix; }

    public String getLockFileName() { return lockFileName; }
    public void setLockFileName(String lockFileName) { this.lockFileName = lockFileName; }

    public Long getHousekeepingInterval() { return housekeepingInterval; }
    public void setHousekeepingInterval(Long housekeepingInterval) { this.housekeepingInterval = housekeepingInterval; }

    public Long getHousekeepingTimeBudget() { return housekeepingTimeBudget; }
    public void setHousekeepingTimeBudget(Long housekeepingTimeBudget) { this.housekeepingTimeBudget = housekeepingTimeBudget; }

    public Long getHousekeepingMaximumTimeBudget() { return housekeepingMaximumTimeBudget; }
    public void setHousekeepingMaximumTimeBudget(Long housekeepingMaximumTimeBudget) { this.housekeepingMaximumTimeBudget = housekeepingMaximumTimeBudget; }

    public Boolean getHousekeepingAdaptive() { return housekeepingAdaptive; }
    public void setHousekeepingAdaptive(Boolean housekeepingAdaptive) { this.housekeepingAdaptive = housekeepingAdaptive; }

    public Long getHousekeepingIncreaseThreshold() { return housekeepingIncreaseThreshold; }
    public void setHousekeepingIncreaseThreshold(Long housekeepingIncreaseThreshold) { this.housekeepingIncreaseThreshold = housekeepingIncreaseThreshold; }

    public Long getHousekeepingIncreaseAmount() { return housekeepingIncreaseAmount; }
    public void setHousekeepingIncreaseAmount(Long housekeepingIncreaseAmount) { this.housekeepingIncreaseAmount = housekeepingIncreaseAmount; }

    public Long getEntityCacheThreshold() { return entityCacheThreshold; }
    public void setEntityCacheThreshold(Long entityCacheThreshold) { this.entityCacheThreshold = entityCacheThreshold; }

    public Long getEntityCacheTimeout() { return entityCacheTimeout; }
    public void setEntityCacheTimeout(Long entityCacheTimeout) { this.entityCacheTimeout = entityCacheTimeout; }

    public Integer getDataFileMinimumSize() { return dataFileMinimumSize; }
    public void setDataFileMinimumSize(Integer dataFileMinimumSize) { this.dataFileMinimumSize = dataFileMinimumSize; }

    public Integer getDataFileMaximumSize() { return dataFileMaximumSize; }
    public void setDataFileMaximumSize(Integer dataFileMaximumSize) { this.dataFileMaximumSize = dataFileMaximumSize; }

    public Double getDataFileMinimumUseRatio() { return dataFileMinimumUseRatio; }
    public void setDataFileMinimumUseRatio(Double dataFileMinimumUseRatio) { this.dataFileMinimumUseRatio = dataFileMinimumUseRatio; }

    public Boolean getDataFileCleanupHeadFile() { return dataFileCleanupHeadFile; }
    public void setDataFileCleanupHeadFile(Boolean dataFileCleanupHeadFile) { this.dataFileCleanupHeadFile = dataFileCleanupHeadFile; }

    public Integer getTransactionFileMaximumSize() { return transactionFileMaximumSize; }
    public void setTransactionFileMaximumSize(Integer transactionFileMaximumSize) { this.transactionFileMaximumSize = transactionFileMaximumSize; }

    public String getAzureConnectionString() { return azureConnectionString; }
    public void setAzureConnectionString(String azureConnectionString) { this.azureConnectionString = azureConnectionString; }

    public String getAzureContainer() { return azureContainer; }
    public void setAzureContainer(String azureContainer) { this.azureContainer = azureContainer; }
}

