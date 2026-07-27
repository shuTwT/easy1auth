PRAGMA defer_foreign_keys=ON;
PRAGMA foreign_keys=OFF;

CREATE TABLE "new_social_accounts" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "provider" TEXT NOT NULL,
    "providerId" TEXT NOT NULL,
    "accessToken" TEXT,
    "refreshToken" TEXT,
    "profile" JSONB,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    CONSTRAINT "social_accounts_tenantId_fkey" FOREIGN KEY ("tenantId") REFERENCES "tenants" ("id") ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT "social_accounts_userId_fkey" FOREIGN KEY ("userId") REFERENCES "users" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

INSERT INTO "new_social_accounts" (
    "accessToken",
    "createdAt",
    "id",
    "profile",
    "provider",
    "providerId",
    "refreshToken",
    "tenantId",
    "updatedAt",
    "userId"
)
SELECT
    social_accounts."accessToken",
    social_accounts."createdAt",
    social_accounts."id",
    social_accounts."profile",
    social_accounts."provider",
    social_accounts."providerId",
    social_accounts."refreshToken",
    users."tenantId",
    social_accounts."updatedAt",
    social_accounts."userId"
FROM "social_accounts"
INNER JOIN "users" ON users."id" = social_accounts."userId";

DROP TABLE "social_accounts";
ALTER TABLE "new_social_accounts" RENAME TO "social_accounts";
CREATE UNIQUE INDEX "social_accounts_tenantId_provider_providerId_key" ON "social_accounts"("tenantId", "provider", "providerId");
CREATE INDEX "social_accounts_tenantId_idx" ON "social_accounts"("tenantId");
CREATE INDEX "social_accounts_userId_idx" ON "social_accounts"("userId");

PRAGMA foreign_keys=ON;
PRAGMA defer_foreign_keys=OFF;
