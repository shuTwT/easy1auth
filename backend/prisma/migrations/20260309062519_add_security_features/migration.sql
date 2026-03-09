/*
  Warnings:

  - You are about to drop the `login_attempts` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `security_settings` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `user_mfas` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the column `tenantId` on the `password_histories` table. All the data in the column will be lost.

*/
-- DropIndex
DROP INDEX "login_attempts_ipAddress_idx";

-- DropIndex
DROP INDEX "login_attempts_userId_idx";

-- DropIndex
DROP INDEX "login_attempts_tenantId_createdAt_idx";

-- DropIndex
DROP INDEX "security_settings_tenantId_idx";

-- DropIndex
DROP INDEX "security_settings_tenantId_key";

-- DropIndex
DROP INDEX "user_mfas_userId_provider_key";

-- DropIndex
DROP INDEX "user_mfas_tenantId_idx";

-- DropIndex
DROP INDEX "user_mfas_userId_idx";

-- DropTable
PRAGMA foreign_keys=off;
DROP TABLE "login_attempts";
PRAGMA foreign_keys=on;

-- DropTable
PRAGMA foreign_keys=off;
DROP TABLE "security_settings";
PRAGMA foreign_keys=on;

-- DropTable
PRAGMA foreign_keys=off;
DROP TABLE "user_mfas";
PRAGMA foreign_keys=on;

-- CreateTable
CREATE TABLE "mfa_tokens" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "token" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "used" BOOLEAN NOT NULL DEFAULT false,
    "expiresAt" DATETIME NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "mfa_tokens_userId_fkey" FOREIGN KEY ("userId") REFERENCES "admins" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "security_policies" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "passwordMinLength" INTEGER NOT NULL DEFAULT 8,
    "passwordRequireUpper" BOOLEAN NOT NULL DEFAULT true,
    "passwordRequireLower" BOOLEAN NOT NULL DEFAULT true,
    "passwordRequireNumber" BOOLEAN NOT NULL DEFAULT true,
    "passwordRequireSpecial" BOOLEAN NOT NULL DEFAULT true,
    "passwordMaxAge" INTEGER NOT NULL DEFAULT 90,
    "passwordHistoryCount" INTEGER NOT NULL DEFAULT 5,
    "mfaRequired" BOOLEAN NOT NULL DEFAULT false,
    "mfaType" TEXT NOT NULL DEFAULT 'totp',
    "sessionTimeout" INTEGER NOT NULL DEFAULT 7200,
    "loginAttemptsLimit" INTEGER NOT NULL DEFAULT 5,
    "lockoutDuration" INTEGER NOT NULL DEFAULT 1800,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);

-- RedefineTables
PRAGMA defer_foreign_keys=ON;
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_admins" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "currentTenantId" TEXT,
    "username" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "phone" TEXT,
    "password" TEXT NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'active',
    "lastLoginAt" DATETIME,
    "passwordChangedAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "mfaEnabled" BOOLEAN NOT NULL DEFAULT false,
    "mfaSecret" TEXT,
    "mfaType" TEXT,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    CONSTRAINT "admins_currentTenantId_fkey" FOREIGN KEY ("currentTenantId") REFERENCES "tenants" ("id") ON DELETE SET NULL ON UPDATE CASCADE
);
INSERT INTO "new_admins" ("createdAt", "currentTenantId", "email", "id", "lastLoginAt", "password", "phone", "status", "updatedAt", "username") SELECT "createdAt", "currentTenantId", "email", "id", "lastLoginAt", "password", "phone", "status", "updatedAt", "username" FROM "admins";
DROP TABLE "admins";
ALTER TABLE "new_admins" RENAME TO "admins";
CREATE UNIQUE INDEX "admins_username_key" ON "admins"("username");
CREATE UNIQUE INDEX "admins_email_key" ON "admins"("email");
CREATE INDEX "admins_currentTenantId_idx" ON "admins"("currentTenantId");
CREATE TABLE "new_password_histories" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "userId" TEXT NOT NULL,
    "password" TEXT NOT NULL,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "password_histories_userId_fkey" FOREIGN KEY ("userId") REFERENCES "admins" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);
INSERT INTO "new_password_histories" ("createdAt", "id", "password", "userId") SELECT "createdAt", "id", "password", "userId" FROM "password_histories";
DROP TABLE "password_histories";
ALTER TABLE "new_password_histories" RENAME TO "password_histories";
CREATE INDEX "password_histories_userId_idx" ON "password_histories"("userId");
PRAGMA foreign_keys=ON;
PRAGMA defer_foreign_keys=OFF;

-- CreateIndex
CREATE INDEX "mfa_tokens_userId_idx" ON "mfa_tokens"("userId");

-- CreateIndex
CREATE INDEX "mfa_tokens_token_idx" ON "mfa_tokens"("token");

-- CreateIndex
CREATE UNIQUE INDEX "security_policies_tenantId_key" ON "security_policies"("tenantId");

-- CreateIndex
CREATE INDEX "security_policies_tenantId_idx" ON "security_policies"("tenantId");
