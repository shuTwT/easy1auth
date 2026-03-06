import express from 'express'
import cors from 'cors'
import helmet from 'helmet'
import morgan from 'morgan'
import dotenv from 'dotenv'
import { errorHandler } from './middleware/errorHandler'
import { auditMiddleware } from './middleware/audit.middleware'
import authRoutes from './routes/auth.routes'
import tenantRoutes from './routes/tenant.routes'
import userRoutes from './routes/user.routes'
import applicationRoutes from './routes/application.routes'
import oauth2Routes from './routes/oauth2.routes'
import auditRoutes from './routes/audit.routes'
import groupRoutes from './routes/group.routes'
import positionRoutes from './routes/position.routes'
import roleRoutes from './routes/role.routes'
import socialIdentityProviderRoutes from './routes/socialIdentityProvider.routes'
import brandSettingsRoutes from './routes/brandSettings.routes'

dotenv.config()

const app = express()
const PORT = process.env.PORT || 18848

app.use(helmet())
app.use(cors())
app.use(morgan('combined'))
app.use(express.json())
app.use(express.urlencoded({ extended: true }))

app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() })
})

app.use('/api/auth', authRoutes)
app.use('/api/tenants', tenantRoutes)
app.use('/api/users', userRoutes)
app.use('/api/applications', applicationRoutes)
app.use('/oauth2', oauth2Routes)
app.use('/api/audit-logs', auditRoutes)
app.use('/api/groups', groupRoutes)
app.use('/api/positions', positionRoutes)
app.use('/api/roles', roleRoutes)
app.use('/api/social-identity-providers', socialIdentityProviderRoutes)
app.use('/api/brand-settings', brandSettingsRoutes)

app.use(auditMiddleware())

app.use(errorHandler)

app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`)
})

export default app
