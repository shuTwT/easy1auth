import { UserGroup, User } from '@prisma/client'
import prisma from '../lib/prisma'
import {
  CreateGroupDto,
  UpdateGroupDto,
  GroupQueryDto,
  GroupListResponse,
  GroupTreeResponse,
  AddMembersDto,
  RemoveMembersDto,
  AddAdminsDto,
  RemoveAdminsDto,
  GroupMembersResponse,
  GroupStats,
  GroupType
} from '../types/group.types'
import { AppError } from '../middleware/errorHandler'
import { v4 as uuidv4 } from 'uuid'

export class GroupService {
  async create(tenantId: string, data: CreateGroupDto): Promise<UserGroup> {
    const existingGroup = await prisma.userGroup.findFirst({
      where: {
        tenantId,
        name: data.name
      }
    })

    if (existingGroup) {
      throw new AppError('用户组名称已存在', 400)
    }

    if (data.parentId) {
      const parentGroup = await prisma.userGroup.findFirst({
        where: {
          id: data.parentId,
          tenantId
        }
      })

      if (!parentGroup) {
        throw new AppError('父级用户组不存在', 404)
      }
    }

    const group = await prisma.userGroup.create({
      data: {
        id: uuidv4(),
        tenantId,
        name: data.name,
        description: data.description ?? null,
        type: data.type ?? 'team',
        parentId: data.parentId ?? null
      },
      include: {
        parent: true,
        _count: {
          select: {
            members: true,
            admins: true,
            children: true
          }
        }
      }
    })

    return group
  }

  async findById(tenantId: string, id: string): Promise<UserGroup> {
    const group = await prisma.userGroup.findFirst({
      where: {
        id,
        tenantId
      },
      include: {
        parent: true,
        _count: {
          select: {
            members: true,
            admins: true,
            children: true
          }
        }
      }
    })

    if (!group) {
      throw new AppError('用户组不存在', 404)
    }

    return group
  }

  async findAll(tenantId: string, query: GroupQueryDto): Promise<GroupListResponse> {
    const page = query.page || 1
    const pageSize = query.pageSize || 10
    const skip = (page - 1) * pageSize

    const where: any = {
      tenantId
    }

    if (query.name) {
      where.name = {
        contains: query.name
      }
    }

    if (query.type) {
      where.type = query.type
    }

    if (query.parentId !== undefined) {
      where.parentId = query.parentId || null
    }

    const [groups, total] = await Promise.all([
      prisma.userGroup.findMany({
        where,
        skip,
        take: pageSize,
        orderBy: { createdAt: 'desc' },
        include: {
          parent: true,
          _count: {
            select: {
              members: true,
              admins: true,
              children: true
            }
          }
        }
      }),
      prisma.userGroup.count({ where })
    ])

    return {
      groups,
      total,
      page,
      pageSize
    }
  }

  async update(tenantId: string, id: string, data: UpdateGroupDto): Promise<UserGroup> {
    await this.findById(tenantId, id)

    if (data.name) {
      const existingGroup = await prisma.userGroup.findFirst({
        where: {
          tenantId,
          name: data.name,
          NOT: { id }
        }
      })

      if (existingGroup) {
        throw new AppError('用户组名称已存在', 400)
      }
    }

    if (data.parentId) {
      if (data.parentId === id) {
        throw new AppError('不能将自己设置为父级', 400)
      }

      const parentGroup = await prisma.userGroup.findFirst({
        where: {
          id: data.parentId,
          tenantId
        }
      })

      if (!parentGroup) {
        throw new AppError('父级用户组不存在', 404)
      }

      const isDescendant = await this.isDescendant(tenantId, id, data.parentId)
      if (isDescendant) {
        throw new AppError('不能将子级设置为父级', 400)
      }
    }

    const group = await prisma.userGroup.update({
      where: { id },
      data: {
        name: data.name,
        description: data.description,
        type: data.type,
        parentId: data.parentId === undefined ? undefined : (data.parentId || null)
      },
      include: {
        parent: true,
        _count: {
          select: {
            members: true,
            admins: true,
            children: true
          }
        }
      }
    })

    return group
  }

  async delete(tenantId: string, id: string): Promise<void> {
    await this.findById(tenantId, id)

    const childCount = await prisma.userGroup.count({
      where: {
        parentId: id
      }
    })

    if (childCount > 0) {
      throw new AppError('该用户组下还有子组，无法删除', 400)
    }

    await prisma.userGroup.delete({
      where: { id }
    })
  }

  async getTree(tenantId: string): Promise<GroupTreeResponse[]> {
    const groups = await prisma.userGroup.findMany({
      where: { tenantId },
      include: {
        _count: {
          select: {
            members: true,
            admins: true
          }
        }
      },
      orderBy: { name: 'asc' }
    })

    const groupMap = new Map<string, GroupTreeResponse>()
    groups.forEach(group => {
      groupMap.set(group.id, {
        id: group.id,
        name: group.name,
        description: group.description,
        type: group.type,
        parentId: group.parentId,
        children: [],
        memberCount: group._count.members,
        adminCount: group._count.admins
      })
    })

    const rootGroups: GroupTreeResponse[] = []
    groups.forEach(group => {
      const node = groupMap.get(group.id)!
      if (group.parentId && groupMap.has(group.parentId)) {
        groupMap.get(group.parentId)!.children.push(node)
      } else {
        rootGroups.push(node)
      }
    })

    return rootGroups
  }

  async addMembers(tenantId: string, groupId: string, data: AddMembersDto): Promise<void> {
    await this.findById(tenantId, groupId)

    const users = await prisma.user.findMany({
      where: {
        id: { in: data.userIds },
        tenantId
      }
    })

    if (users.length !== data.userIds.length) {
      throw new AppError('部分用户不存在或不属于当前租户', 400)
    }

    await prisma.userGroup.update({
      where: { id: groupId },
      data: {
        members: {
          connect: data.userIds.map(id => ({ id }))
        }
      }
    })
  }

  async removeMembers(tenantId: string, groupId: string, data: RemoveMembersDto): Promise<void> {
    await this.findById(tenantId, groupId)

    await prisma.userGroup.update({
      where: { id: groupId },
      data: {
        members: {
          disconnect: data.userIds.map(id => ({ id }))
        }
      }
    })
  }

  async addAdmins(tenantId: string, groupId: string, data: AddAdminsDto): Promise<void> {
    await this.findById(tenantId, groupId)

    const users = await prisma.user.findMany({
      where: {
        id: { in: data.userIds },
        tenantId
      }
    })

    if (users.length !== data.userIds.length) {
      throw new AppError('部分用户不存在或不属于当前租户', 400)
    }

    await prisma.userGroup.update({
      where: { id: groupId },
      data: {
        admins: {
          connect: data.userIds.map(id => ({ id }))
        }
      }
    })
  }

  async removeAdmins(tenantId: string, groupId: string, data: RemoveAdminsDto): Promise<void> {
    await this.findById(tenantId, groupId)

    await prisma.userGroup.update({
      where: { id: groupId },
      data: {
        admins: {
          disconnect: data.userIds.map(id => ({ id }))
        }
      }
    })
  }

  async getMembers(tenantId: string, groupId: string): Promise<GroupMembersResponse> {
    await this.findById(tenantId, groupId)

    const group = await prisma.userGroup.findUnique({
      where: { id: groupId },
      include: {
        members: true,
        admins: true
      }
    })

    return {
      members: group!.members,
      admins: group!.admins,
      total: group!.members.length
    }
  }

  async getStats(tenantId: string): Promise<GroupStats> {
    const [
      totalGroups,
      teamGroups,
      departmentGroups,
      projectGroups,
      organizationGroups,
      rootGroups
    ] = await Promise.all([
      prisma.userGroup.count({ where: { tenantId } }),
      prisma.userGroup.count({ where: { tenantId, type: 'team' } }),
      prisma.userGroup.count({ where: { tenantId, type: 'department' } }),
      prisma.userGroup.count({ where: { tenantId, type: 'project' } }),
      prisma.userGroup.count({ where: { tenantId, type: 'organization' } }),
      prisma.userGroup.count({ where: { tenantId, parentId: null } })
    ])

    return {
      totalGroups,
      teamGroups,
      departmentGroups,
      projectGroups,
      organizationGroups,
      rootGroups
    }
  }

  private async isDescendant(tenantId: string, ancestorId: string, descendantId: string): Promise<boolean> {
    const descendant = await prisma.userGroup.findFirst({
      where: {
        id: descendantId,
        tenantId
      }
    })

    if (!descendant || !descendant.parentId) {
      return false
    }

    if (descendant.parentId === ancestorId) {
      return true
    }

    return this.isDescendant(tenantId, ancestorId, descendant.parentId)
  }
}

export default new GroupService()
