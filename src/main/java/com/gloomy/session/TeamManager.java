package com.gloomy.session;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;

public class TeamManager
{
    private final List<Team> teams = new ArrayList<>();

    private TeamManager() {}

    /**
     * Constructs a new TeamManager with a handle on a specified client list and with max session total of players and
     * a specified amount of teams
     * @param clientList Handle to the list of clients to work with (NEEDED?)
     * @param maxPlayers The maximum amount of players allowed in the session
     * @param amountOfTeams The amount of teams that should be created for this manager to manage
     */
    public TeamManager(List<Client> clientList, int maxPlayers, int amountOfTeams)
    {
        List<Client> sessionClientList = clientList;
        int amountOfTeams1 = amountOfTeams;
        int maxPlayers1 = maxPlayers;

        for (int i = 0; i < amountOfTeams; i++)
            teams.add(new Team(i));
    }

    /**
     * Adds a client after finding the team with the least players, and returns the Team that the client is added to.
     * @param client The client that should be added to a team.
     * @return The team that the client is added to
     */
    public void addClientGetTeam(Client client)
    {
        Team clientsTeam = findTeamWithLeastPlayers();
        client.setTeam(clientsTeam);
        clientsTeam.teamMembers.add(client);
    }

    private Team findTeamWithLeastPlayers()
    {
        Team minTeam = teams.get(0);

        for (Team team : teams)
        {
            Logger.info("Compared Team Size {} vs First Team Size {}", team.teamMembers.size(), minTeam.teamMembers.size());
            if (team.teamMembers.size() < minTeam.teamMembers.size())
                minTeam = team;
        }

        return minTeam; // Just return the first team if all teams have the same amount of players.
    }

    /**
     * Returns the team with the specified id
     * @param id The id to find the team of
     * @return The team with the id specified
     */
    private Team getTeamFromId(int id)
    {
        for (Team team : teams)
            if (team.id == id)
                return team;

        Logger.warn("No team with id {} was found, returning null.", id);
        return null;
    }

    /**
     * Team class is a container for clients and team id
     */
    class Team
    {
        public int id = 0;
        public final List<Client> teamMembers = new ArrayList<>();

        public Team(int id)
        {
            this.id = id;
        }

        public void removeMember(Client client)
        {
            teamMembers.remove(client);
        }
    }

    public int getNumberOfTeams()
    {
        return teams.size();
    }
}
