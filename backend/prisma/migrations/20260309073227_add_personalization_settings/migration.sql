-- CreateTable
CREATE TABLE "custom_domains" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "domain" TEXT NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'pending',
    "verificationMethod" TEXT NOT NULL DEFAULT 'dns',
    "verificationToken" TEXT,
    "verifiedAt" DATETIME,
    "sslStatus" TEXT NOT NULL DEFAULT 'none',
    "sslCertificate" TEXT,
    "sslPrivateKey" TEXT,
    "sslExpiresAt" DATETIME,
    "errorMessage" TEXT,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    CONSTRAINT "custom_domains_tenantId_fkey" FOREIGN KEY ("tenantId") REFERENCES "tenants" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "message_templates" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "type" TEXT NOT NULL,
    "code" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "subject" TEXT,
    "content" TEXT NOT NULL,
    "variables" JSONB,
    "isDefault" BOOLEAN NOT NULL DEFAULT false,
    "status" TEXT NOT NULL DEFAULT 'active',
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    CONSTRAINT "message_templates_tenantId_fkey" FOREIGN KEY ("tenantId") REFERENCES "tenants" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "login_styles" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "tenantId" TEXT NOT NULL,
    "logo" TEXT,
    "logoDark" TEXT,
    "backgroundImage" TEXT,
    "backgroundColor" TEXT NOT NULL DEFAULT '#f5f7fa',
    "primaryColor" TEXT NOT NULL DEFAULT '#0369A1',
    "title" TEXT NOT NULL DEFAULT 'Easy1Auth',
    "subtitle" TEXT NOT NULL DEFAULT '企业级身份管理平台',
    "customCSS" TEXT,
    "loginMethods" JSONB,
    "socialProviders" JSONB,
    "createdAt" DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" DATETIME NOT NULL,
    CONSTRAINT "login_styles_tenantId_fkey" FOREIGN KEY ("tenantId") REFERENCES "tenants" ("id") ON DELETE CASCADE ON UPDATE CASCADE
);

-- CreateIndex
CREATE UNIQUE INDEX "custom_domains_domain_key" ON "custom_domains"("domain");

-- CreateIndex
CREATE INDEX "custom_domains_tenantId_idx" ON "custom_domains"("tenantId");

-- CreateIndex
CREATE INDEX "custom_domains_domain_idx" ON "custom_domains"("domain");

-- CreateIndex
CREATE INDEX "message_templates_tenantId_idx" ON "message_templates"("tenantId");

-- CreateIndex
CREATE UNIQUE INDEX "message_templates_tenantId_type_code_key" ON "message_templates"("tenantId", "type", "code");

-- CreateIndex
CREATE UNIQUE INDEX "login_styles_tenantId_key" ON "login_styles"("tenantId");
