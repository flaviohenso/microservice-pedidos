#!/bin/bash

# Script para fazer push para o GitHub
# Execute após criar o repositório em: https://github.com/new

echo "🚀 Fazendo push do microservice-pedidos para o GitHub..."
echo ""

# Adiciona o remote origin
git remote add origin https://github.com/flaviohenso/microservice-pedidos.git

# Faz push da branch main
echo "📤 Enviando branch main..."
git push -u origin main

# Faz push da branch develop
echo "📤 Enviando branch develop..."
git push -u origin develop

echo ""
echo "✅ Push concluído com sucesso!"
echo ""
echo "🌐 Acesse seu repositório em:"
echo "   https://github.com/flaviohenso/microservice-pedidos"
echo ""
echo "📋 Próximos passos:"
echo "   1. Configure branch develop como default no GitHub (Settings > Branches)"
echo "   2. Configure branch protection rules para main"
echo "   3. Adicione descrição e topics no repositório"




