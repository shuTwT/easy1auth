-- CreateTable
CREATE TABLE "security_settings" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "passwordMinLength" INTEGER NOT NULL DEFAULT 8,
    "passwordRequireUppercase" BOOLEAN NOT NULL DEFAULT true,
    "passwordRequireLowercase" BOOLEAN NOT NULL DEFAULT true,
    "passwordRequireNumber" BOOLEAN NOT NULL DEFAULT true,
    "passwordRequireSpecial" BOOLEAN NOT NULL DEFAULT false,
    "passwordSpecialChars" TEXT NOT NULL DEFAULT '!@#$%^&*()_+-=[]{}|;:,.<>?',
    "passwordMaxAge" INTEGER NOT NULL DEFAULT 90,
    "passwordHistoryCount" INTEGER NOT NULL DEFAULT 5,
    "passwordPreventReuse" BOOLEAN NOT NULL DEFAULT true,
    "mfaEnabled" BOOLEAN NOT NULL DEFAULT false,
    "mfaRequired" BOOLEAN NOT NULL DEFAULT false,
    "mfaProviders" JSONB NOT NULL DEFAULT [],
    "loginAttemptLimit" INTEGER NOT NULL DEFAULT 5,
    "lockoutDuration" INTEGER NOT NULL DEFAULT 30,
    "sessionTimeout" INTEGER NOT NULL DEFAULT 1440,
    "ipWhitelistEnabled" BOOLEAN NOT NULL DEFAULT false,
    "ipWhitelist" JSONB NOT NULL DEFAULT [],
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);

-- CreateTable
CREATE TABLE "password_histories" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "tenantId" TEXT NOT NULL,
    "password" TEXT NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- CreateTable
CREATE TABLE "user_mfas" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "tenantId" TEXT NOT NULL,
    "provider" TEXT NOT NULL,
    "secret" TEXT,
    "backupCodes" JSONB,
    "enabled" BOOLEAN NOT NULL DEFAULT false,
    "verifiedAt" DATETIME,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);

-- CreateTable
CREATE TABLE "login_attempts" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT,
    "tenantId" TEXT NOT NULL,
    "username" TEXT,
    "email" TEXT,
    "ipAddress" TEXT NOT NULL,
    "userAgent" TEXT,
    "success" BOOLEAN NOT NULL,
    "failureReason" TEXT,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- CreateIndex
CREATE UNIQUE INDEX "security_settings_tenantId_key" ON "security_settings"("tenantId");

-- CreateIndex
CREATE INDEX "security_settings_tenantId_idx" ON "security_settings"("tenantId");

-- CreateIndex
CREATE INDEX "password_histories_userId_idx" ON "password_histories"("userId");

-- CreateIndex
CREATE INDEX "password_histories_tenantId_idx" ON "password_histories"("tenantId");

-- CreateIndex
CREATE INDEX "user_mfas_userId_idx" ON "user_mfas"("userId");

-- CreateIndex
CREATE INDEX "user_mfas_tenantId_idx" ON "user_mfas"("tenantId");

-- CreateIndex
CREATE UNIQUE INDEX "user_mfas_userId_provider_key" ON "user_mfas"("userId", "provider");

-- CreateIndex
CREATE INDEX "login_attempts_tenantId_createdAt_idx" ON "login_attempts"("tenantId", "createdAt");

-- CreateIndex
CREATE INDEX "login_attempts_userId_idx" ON "login_attempts"("userId");

-- CreateIndex
CREATE INDEX "login_attempts_ipAddress_idx" ON "login_attempts"("ipAddress");
