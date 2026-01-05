# Guia de Setup do GitHub

## 🎯 Resumo

Este guia te ajudará a enviar o código do microservice-pedidos para o GitHub.

---

## 📋 Método 1: Criar Novo Repositório (Recomendado)

### Passo 1: Criar repositório no GitHub

1. Acesse: https://github.com/new
2. Configure:
   - **Nome**: `microservice-pedidos`
   - **Descrição**: `Microserviço de Pedidos - Clean Architecture com Outbox Pattern`
   - **Visibilidade**: Public ou Private
   - ⚠️ **IMPORTANTE**: Deixe o repositório completamente vazio (não marque nenhuma opção)
3. Clique em "Create repository"

### Passo 2: Executar o script de push

```bash
cd /home/flavio/Projetos/microservice-pedidos
./push-to-github.sh
```

Ou execute manualmente:

```bash
cd /home/flavio/Projetos/microservice-pedidos

# Adicionar remote
git remote add origin https://github.com/flaviohenso/microservice-pedidos.git

# Push branch main
git push -u origin main

# Push branch develop
git push -u origin develop
```

---

## 🔄 Método 2: Se o repositório já existe

Se você já criou o repositório anteriormente:

```bash
cd /home/flavio/Projetos/microservice-pedidos

# Verificar se remote já existe
git remote -v

# Se já existe, remover
git remote remove origin

# Adicionar novamente
git remote add origin https://github.com/flaviohenso/microservice-pedidos.git

# Forçar push (use com cuidado!)
git push -u origin main --force
git push -u origin develop --force
```

---

## 🔐 Autenticação

### Se pedir credenciais:

O GitHub não aceita mais senha via HTTPS. Use uma destas opções:

#### Opção A: Personal Access Token (Recomendado)

1. Acesse: https://github.com/settings/tokens
2. Clique em "Generate new token (classic)"
3. Dê um nome: `microservice-pedidos-push`
4. Marque: `repo` (Full control of private repositories)
5. Clique em "Generate token"
6. **Copie o token** (só aparece uma vez!)
7. Quando o git pedir senha, cole o token

#### Opção B: SSH (Mais seguro)

```bash
# Gerar chave SSH (se não tiver)
ssh-keygen -t ed25519 -C "seu-email@example.com"

# Copiar chave pública
cat ~/.ssh/id_ed25519.pub

# Adicionar em: https://github.com/settings/keys

# Mudar remote para SSH
git remote set-url origin git@github.com:flaviohenso/microservice-pedidos.git

# Fazer push
git push -u origin main
git push -u origin develop
```

---

## ✅ Verificação

Após o push, verifique:

1. Acesse: https://github.com/flaviohenso/microservice-pedidos
2. Você deve ver:
   - ✓ Branch `main`
   - ✓ Branch `develop`
   - ✓ 49 arquivos
   - ✓ README.md bem formatado
   - ✓ Histórico de commits

---

## ⚙️ Configurações Recomendadas no GitHub

### 1. Definir branch padrão

1. Vá em: `Settings` > `Branches`
2. Em "Default branch", selecione `develop`
3. Clique em "Update"

### 2. Proteção de branches

#### Proteger `main`:

1. Vá em: `Settings` > `Branches` > `Adefaultdd rule`
2. Branch name pattern: `main`
3. Marque:
   - ☑ Require a pull request before merging
   - ☑ Require approvals (1)
   - ☑ Dismiss stale pull request approvals
   - ☑ Require status checks to pass
4. Clique em "Create"

#### Proteger `develop`:

1. Adicione outra regra para `develop`
2. Configure proteções similares

### 3. Adicionar descrição e topics

1. Vá em "About" (lateral direita)
2. Clique em ⚙️
3. Adicione:
   - **Description**: `Microserviço de Pedidos - Clean Architecture, Outbox Pattern, RabbitMQ, PostgreSQL`
   - **Topics**: `java`, `spring-boot`, `clean-architecture`, `hexagonal-architecture`, `outbox-pattern`, `rabbitmq`, `postgresql`, `microservices`, `docker`
4. Salve

---

## 🚨 Problemas Comuns

### Erro: "remote origin already exists"

```bash
git remote remove origin
git remote add origin https://github.com/flaviohenso/microservice-pedidos.git
```

### Erro: "failed to push some refs"

```bash
# Puxar mudanças primeiro (se houver)
git pull origin main --allow-unrelated-histories

# Ou forçar push (CUIDADO: sobrescreve tudo)
git push -u origin main --force
```

### Erro: "Authentication failed"

Use Personal Access Token ou SSH (veja seção de Autenticação acima)

---

## 📞 Ajuda

Se encontrar problemas:

1. Verifique status: `git status`
2. Verifique remotes: `git remote -v`
3. Verifique branches: `git branch -a`
4. Logs: `git log --oneline -5`

---

## 🎉 Pronto!

Após o push bem-sucedido, compartilhe seu repositório:

**URL**: https://github.com/flaviohenso/microservice-pedidos

Clone em outras máquinas:

```bash
git clone https://github.com/flaviohenso/microservice-pedidos.git
cd microservice-pedidos
git checkout develop
```




