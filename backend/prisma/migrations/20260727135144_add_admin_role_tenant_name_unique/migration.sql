/*
  Warnings:

  - A unique constraint covering the columns `[tenantId,name]` on the table `admin_roles` will be added. If there are existing duplicate values, this will fail.

*/
-- CreateIndex
CREATE UNIQUE INDEX "admin_roles_tenantId_name_key" ON "admin_roles"("tenantId", "name");
