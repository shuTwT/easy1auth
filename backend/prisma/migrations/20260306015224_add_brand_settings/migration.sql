/*
  Warnings:

  - You are about to drop the column `adminPanelCss` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `adminPanelLogo` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `adminPanelTitle` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `favicon` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `headerColor` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `loginPageBackground` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `loginPageCss` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `loginPageTitle` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `primaryColor` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `privacyPolicyContent` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `privacyPolicyUrl` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `sidebarColor` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `termsOfServiceContent` on the `tenants` table. All the data in the column will be lost.
  - You are about to drop the column `termsOfServiceUrl` on the `tenants` table. All the data in the column will be lost.

*/
-- RedefineTables
PRAGMA defer_foreign_keys=ON;
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_social_identity_providers" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "clientId" TEXT NOT NULL,
    "clientSecret" TEXT NOT NULL,
    "authorizationEndpoint" TEXT NOT NULL,
    "tokenEndpoint" TEXT NOT NULL,
    "userInfoEndpoint" TEXT NOT NULL,
    "scope" JSONB NOT NULL,
    "attributeMapping" JSONB,
    "status" TEXT NOT NULL DEFAULT 'active',
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    CONSTRAINT "social_identity_providers_tenantId_fkey" FOREIGN KEY ("tenantId") REFERENCES "tenants" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);
INSERT INTO "new_social_identity_providers" ("attributeMapping", "authorizationEndpoint", "clientId", "clientSecret", "createdAt", "id", "name", "scope", "status", "tenantId", "tokenEndpoint", "type", "updatedAt", "userInfoEndpoint") SELECT "attributeMapping", "authorizationEndpoint", "clientId", "clientSecret", "createdAt", "id", "name", "scope", "status", "tenantId", "tokenEndpoint", "type", "updatedAt", "userInfoEndpoint" FROM "social_identity_providers";
DROP TABLE "social_identity_providers";
ALTER TABLE "new_social_identity_providers" RENAME TO "social_identity_providers";
CREATE INDEX "social_identity_providers_tenantId_idx" ON "social_identity_providers"("tenantId");
CREATE TABLE "new_tenants" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "name" TEXT NOT NULL,
    "logo" TEXT,
    "domain" TEXT,
    "status" TEXT NOT NULL DEFAULT 'active',
    "plan" TEXT NOT NULL DEFAULT 'basic',
    "maxUsers" INTEGER NOT NULL DEFAULT 100,
    "maxApps" INTEGER NOT NULL DEFAULT 10,
    "brandSettings" JSONB,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL
);
INSERT INTO "new_tenants" ("createdAt", "domain", "id", "logo", "maxApps", "maxUsers", "name", "plan", "status", "updatedAt") SELECT "createdAt", "domain", "id", "logo", "maxApps", "maxUsers", "name", "plan", "status", "updatedAt" FROM "tenants";
DROP TABLE "tenants";
ALTER TABLE "new_tenants" RENAME TO "tenants";
CREATE UNIQUE INDEX "tenants_domain_key" ON "tenants"("domain");
PRAGMA foreign_keys=ON;
PRAGMA defer_foreign_keys=OFF;
