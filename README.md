# Privacy-Preserving E-Voting System

> **Academic Research Prototype** — Not certified for production public elections.

A research-grade, end-to-end verifiable, blockchain-based electronic voting system demonstrating privacy-preserving ballot secrecy, split-trust architecture, and cryptographic auditability.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 25 LTS, Spring Boot 4.x |
| Blockchain | Hyperledger Fabric 3.x (Java Chaincode) |
| Database | Supabase (PostgreSQL 17) |
| Frontend | HTML5, Bootstrap 5, JSP, JSTL |
| Build | Maven 3.9+ |
| Security | Spring Security 6, JWT, Argon2id, AES-256-GCM |
| Crypto | JCA / BouncyCastle, SHA-256, Merkle Trees |

## Quick Start

```bash
# Clone
git clone https://github.com/KamaleshwarS31/JavaProject.git
cd JavaProject

# Copy and configure environment (NEVER commit .env)
cp .env.example .env
# Edit .env with your Supabase credentials and Fabric paths

# Build all modules
mvn clean install -DskipTests

# Run migrations
mvn flyway:migrate -pl evoting-platform/database

# Start application
mvn spring-boot:run -pl evoting-platform/backend/evoting-web
```

## Project Structure

```
evoting-platform/
├── backend/
│   ├── evoting-core/          # Domain models, crypto, state machines
│   ├── evoting-security/      # Auth, JWT, RBAC, rate limiting
│   ├── evoting-eligibility/   # Voter eligibility and credentials
│   ├── evoting-ballot/        # Ballot ingestion and nullifiers
│   ├── evoting-blockchain/    # Hyperledger Fabric Gateway integration
│   ├── evoting-audit/         # Audit anchors and verification
│   ├── evoting-tally/         # Tally computation
│   ├── evoting-web/           # Spring Boot main application
│   └── evoting-tests/         # Integration and security tests
├── blockchain/
│   ├── chaincode-java/        # Hyperledger Fabric Java chaincode
│   ├── network/               # Fabric network Docker configuration
│   └── scripts/               # Network management scripts
├── database/
│   ├── migrations/            # Flyway SQL migrations
│   └── verification/          # Schema audit scripts
├── deployment/
│   ├── docker/                # Dockerfiles
│   └── compose/               # Docker Compose files
└── docs/                      # Architecture, security, API docs
```

## Security Invariant

**There is no database or blockchain record that links voter identity to ballot choice.**

The system is built around the principle of **Separation of Identity from Ballot**:
- Trust Domain 1 (Eligibility): Knows voter identity, NOT their vote
- Trust Domain 2 (Ballot Gateway): Receives encrypted vote + nullifier, NOT voter identity
- Trust Domain 3 (Audit): Verifies mathematical proofs, NOT individual votes

## Documentation

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — System design and trust model
- [SECURITY.md](docs/SECURITY.md) — Security controls and threat model
- [THREAT_MODEL.md](docs/THREAT_MODEL.md) — Threat actors and mitigations
- [DATABASE.md](docs/DATABASE.md) — Schema and integrity rules
- [BLOCKCHAIN.md](docs/BLOCKCHAIN.md) — Fabric network and chaincode
- [API.md](docs/API.md) — REST API documentation
- [DEPLOYMENT.md](docs/DEPLOYMENT.md) — Deployment guide
- [LIMITATIONS.md](docs/LIMITATIONS.md) — Known limitations

## Research Status

This prototype is a **public-election-oriented research system**. Production deployment requires jurisdiction-specific legislation, formal security certification, independent auditing, and institutional operational qualifications.

## License

Academic Research Project — See LICENSE
