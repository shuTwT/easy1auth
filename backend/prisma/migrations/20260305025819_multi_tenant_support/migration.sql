/*
  Warnings:

  - You are about to drop the column `tenantId` on the `admins` table. All the data in the column will be lost.

*/
-- CreateTable
CREATE TABLE "admin_tenants" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "adminId" TEXT NOT NULL,
    "tenantId" TEXT NOT NULL,
    "role" TEXT NOT NULL DEFAULT 'owner',
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "admin_tenants_adminId_fkey" FOREIGN KEY ("adminId") REFERENCES "admins" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "admin_tenants_tenantId_fkey" FOREIGN KEY ("tenantId") REFERENCES "tenants" ("id") ON DELETE CASCADE ON UPDATE CASCADE
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
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    CONSTRAINT "admins_currentTenantId_fkey" FOREIGN KEY ("currentTenantId") REFERENCES "tenants" ("id") ON DELETE SET NULL ON UPDATE CASCADE
);
INSERT INTO "new_admins" ("createdAt", "email", "id", "lastLoginAt", "password", "phone", "status", "updatedAt", "username") SELECT "createdAt", "email", "id", "lastLoginAt", "password", "phone", "status", "updatedAt", "username" FROM "admins";
DROP TABLE "admins";
ALTER TABLE "new_admins" RENAME TO "admins";
CREATE UNIQUE INDEX "admins_username_key" ON "admins"("username");
CREATE UNIQUE INDEX "admins_email_key" ON "admins"("email");
CREATE INDEX "admins_currentTenantId_idx" ON "admins"("currentTenantId");
PRAGMA foreign_keys=ON;
PRAGMA defer_foreign_keys=OFF;

-- CreateIndex
CREATE INDEX "admin_tenants_adminId_idx" ON "admin_tenants"("adminId");

-- CreateIndex
CREATE INDEX "admin_tenants_tenantId_idx" ON "admin_tenants"("tenantId");

-- CreateIndex
CREATE UNIQUE INDEX "admin_tenants_adminId_tenantId_key" ON "admin_tenants"("adminId", "tenantId");
